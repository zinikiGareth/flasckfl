package org.flasck.flas.parser;

import java.util.ArrayList;
import java.util.List;

import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.commonBase.ConstPattern;
import org.flasck.flas.commonBase.Locatable;
import org.flasck.flas.errors.ErrorResult;
import org.flasck.flas.parsedForm.ConstructorMatch;
import org.flasck.flas.parsedForm.TuplePattern;
import org.flasck.flas.parsedForm.TypeReference;
import org.flasck.flas.parsedForm.TypedPattern;
import org.flasck.flas.parsedForm.VarPattern;
import org.flasck.flas.tokenizers.PattToken;
import org.flasck.flas.tokenizers.Tokenizable;
import org.flasck.flas.tokenizers.TypeNameToken;

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
				TypeNameToken next = TypeNameToken.qualified(line);
				// Main case is "(Type var)" - and we know types have initial caps
				if (next != null) {
					PattToken after = PattToken.from(line);
					if (after == null)
						return null;
					TypeReference type;
					if (after.type == PattToken.OSB) {
						// TODO: we should actually process this
						Object ty = processPolyArg(next, line);
						if (ty instanceof ErrorResult)
							return ty;
						type = (TypeReference) ty;
						after = PattToken.from(line);
						if (after == null)
							return ErrorResult.oneMessage(line, "unexpected end of pattern");
					} else
						type = new TypeReference(next.location, next.text);
					if (after.type == PattToken.VAR) {
						TypedPattern ret = new TypedPattern(next.location, type, after.location, after.text);
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
							ret.args.add(ret.new Field(field.location, field.text, nestedPatt));
							PattToken punc = PattToken.from(line);
							if (punc.type == PattToken.CCB)
								break;
							else if (punc.type != PattToken.COMMA)
								return null; // an error
						}
					}
					// Likewise magic list syntax is "(P : var)"
				} else {
//					PattToken var = PattToken.from(line);
					Object p;
//					if (var.type == PattToken.ORB) {
						// in this case, we have nested open parens, e.g. ((a,b):l)
						p = tryParsing(line);
//					} else
//						p = simplePattern(var, line);
					if (p == null)
						return null;
					retArr.add(p);
				}
				PattToken sep = PattToken.from(line);
				if (sep == null)
					return null;
				if (sep.type == PattToken.CRB) {
					if (retArr.size() == 1)
						return retArr.get(0);
					else if (tupleCase)
						return new TuplePattern(retArr);
					else if (listCase) {
						return buildListFromPatterns(tok.location.copySetEnd(sep.location.pastEnd()), retArr, false);
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

	protected Object processPolyArg(TypeNameToken ty, Tokenizable line) {
		List<TypeReference> ret = new ArrayList<TypeReference>();
		while (true) {
			PattToken after;
			TypeNameToken tn = TypeNameToken.qualified(line);
			if (tn == null)
				return ErrorResult.oneMessage(line, "type name expected");
			after = PattToken.from(line);
			TypeReference type;
			if (after.type == PattToken.OSB) {
				Object o = processPolyArg(tn, line);
				if (o instanceof ErrorResult)
					return o;
				type = (TypeReference) o;
			} else
				type = new TypeReference(tn.location, tn.text);
			ret.add(type);
			if (after.type == PattToken.CSB) {
				return new TypeReference(ty.location, ty.text, ret);
			} else if (after.type != PattToken.COMMA)
				return ErrorResult.oneMessage(line, "syntax error");
		}
	}
	
	private Object simplePattern(PattToken tok, Tokenizable line) {
		if (tok.type == PattToken.VAR)
			return new VarPattern(tok.location, tok.text);
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
				return new ConstructorMatch(tok.location.copySetEnd(next.location.pastEnd()), "Nil");
			else {
				line.reset(mark); // put the unknown token back on the front of the input
				while (true) {
					Object p = tryParsing(line);
					if (p == null)
						return null; // already had error
					ps.add(p);
					PattToken sep = PattToken.from(line);
					if (sep.type == PattToken.CSB) {
						return buildListFromPatterns(tok.location.copySetEnd(sep.location.pastEnd()), ps, true);
					} else if (sep.type != PattToken.COMMA)
						return null; // this is an error
				}				
			}
		} else 
			return null;
	}

	private Object buildListFromPatterns(InputPosition location, List<Object> ps, boolean addNil) {
		Object ret = addNil?new ConstructorMatch(location, "Nil"):null;
		for (int i=ps.size()-1;i>=0;i--) {
			if (!addNil && ret == null) {
				ret = ps.get(i);
				continue;
			}
			ConstructorMatch tmp = new ConstructorMatch(location, "Cons");
			InputPosition piloc = ((Locatable) ps.get(i)).location();
			tmp.args.add(tmp.new Field(piloc, "head", ps.get(i)));
			tmp.args.add(tmp.new Field(((Locatable) ret).location(), "tail", ret));
			ret = tmp;
		}
		return ret;
	}
}
