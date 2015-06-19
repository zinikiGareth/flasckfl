package org.flasck.flas.parser;

import java.util.ArrayList;
import java.util.List;

import org.flasck.flas.parsedForm.ConstPattern;
import org.flasck.flas.parsedForm.ConstructorMatch;
import org.flasck.flas.parsedForm.TuplePattern;
import org.flasck.flas.parsedForm.TypedPattern;
import org.flasck.flas.parsedForm.VarPattern;
import org.flasck.flas.tokenizers.PattToken;
import org.flasck.flas.tokenizers.Tokenizable;

public class PatternParser implements TryParsing {

	@Override
	public Object tryParsing(Tokenizable line) {
		PattToken tok = PattToken.from(line);
		if (tok == null)
			return null;

		if (tok.type == PattToken.ORB) {
			// This is actually most of the cases
			
			boolean tupleCase = false;
			boolean listCase = false;
			
			List<Object> retArr = new ArrayList<Object>();
			while (true) {
				// The next symbol can be any valid pattern token
				PattToken next = PattToken.from(line);
				// Main case is "(Type var)" - and we know types have initial caps
				if (next.type == PattToken.TYPE) {
					PattToken after = PattToken.from(line);
					if (after.type == PattToken.VAR) {
						TypedPattern ret = new TypedPattern(next.location, next.text, after.location, after.text);
						retArr.add(ret);
					} else if (after.type == PattToken.OCB) {
						// Subsid case is "(Type { ... })" - again with initial caps
						ConstructorMatch ret = new ConstructorMatch(next.location, next.text);
						retArr.add(ret);
						while (true) {
							PattToken field = PattToken.from(line);
							if (field.type != PattToken.VAR)
								return null; // this should be an error
							PattToken colon = PattToken.from(line);
							if (colon.type != PattToken.COLON)
								return null; // this is an error
							Object nestedPatt = tryParsing(line);
							if (nestedPatt == null)
								return null; // there has probably been an error already
							ret.args.add(ret.new Field(field.text, nestedPatt));
							PattToken punc = PattToken.from(line);
							if (punc.type == PattToken.CCB)
								break;
							else if (punc.type != PattToken.COMMA)
								return null; // an error
						}
					}
					// Likewise magic list syntax is "(P : var)"
				} else {
					Object p = simplePattern(next, line);
					if (p == null)
						return null;
					retArr.add(p);
				}
				PattToken sep = PattToken.from(line);
				if (sep.type == PattToken.CRB) {
					if (retArr.size() == 1)
						return retArr.get(0);
					else if (tupleCase)
						return new TuplePattern(retArr);
					else if (listCase) {
						return buildListFromPatterns(retArr, false);
					}
				} else if (sep.type == PattToken.COLON) {
					if (tupleCase)
						return null;
					listCase = true;
				}
				else if (sep.type == PattToken.COMMA) { // Handle tuples
					if (listCase)
						return null;
					tupleCase = true;
				} else 
					return null; // an error
			}
		}  else
			return simplePattern(tok, line);
	}
	
	private Object simplePattern(PattToken tok, Tokenizable line) {
		if (tok.type == PattToken.VAR)
			return new VarPattern(tok.text);
		else if (tok.type == PattToken.TYPE)
			return new ConstructorMatch(tok.location, tok.text);
		else if (tok.type == PattToken.NUMBER)
			return new ConstPattern(tok.location, ConstPattern.INTEGER, tok.text);
		else if (tok.type == PattToken.FALSE || tok.type == PattToken.TRUE)
			return new ConstPattern(tok.location, ConstPattern.BOOLEAN, tok.text);
		else if (tok.type == PattToken.OSB) {
			int mark = line.at();
			PattToken next = PattToken.from(line);
			List<Object> ps = new ArrayList<Object>();
			if (next.type == PattToken.CSB)
				return new ConstructorMatch(tok.location, "Nil");
			else {
				line.reset(mark); // put the unknown token back on the front of the input
				while (true) {
					Object p = tryParsing(line);
					if (p == null)
						return null; // already had error
					ps.add(p);
					PattToken sep = PattToken.from(line);
					if (sep.type == PattToken.CSB) {
						return buildListFromPatterns(ps, true);
					} else if (sep.type != PattToken.COMMA)
						return null; // this is an error
				}				
			}
		} else 
			return null;
	}

	private Object buildListFromPatterns(List<Object> ps, boolean addNil) {
		Object ret = addNil?new ConstructorMatch(null, "Nil"):null;
		for (int i=ps.size()-1;i>=0;i--) {
			if (!addNil && ret == null) {
				ret = ps.get(i);
				continue;
			}
			ConstructorMatch tmp = new ConstructorMatch(null, "Cons");
			tmp.args.add(tmp.new Field("head", ps.get(i)));
			tmp.args.add(tmp.new Field("tail", ret));
			ret = tmp;
		}
		return ret;
	}
}
