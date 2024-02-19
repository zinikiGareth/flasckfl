package org.flasck.flas.parser;

import java.util.ArrayList;
import java.util.List;

import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.errors.ErrorReporter;
import org.flasck.flas.parsedForm.FunctionTypeReference;
import org.flasck.flas.parsedForm.TupleTypeReference;
import org.flasck.flas.parsedForm.TypeReference;
import org.flasck.flas.tokenizers.Tokenizable;
import org.flasck.flas.tokenizers.TypeExprToken;
import org.zinutils.exceptions.NotImplementedException;

public class TypeExprParser {
	private ErrorReporter errors;
	
	public TypeExprParser(ErrorReporter errors) {
		this.errors = errors;
	}
	
	public void tryParsing(Tokenizable line, TDAProvideType pt) {
		TypeExprToken tt = TypeExprToken.from(errors, line);
		if (tt == null)
			return; // not even a valid token (or line ended)
		TypeReference curr = null;
		if (tt.type == TypeExprToken.NAME) {
			curr = new TypeReference(tt.location, tt.text);
		} else if (tt.type == TypeExprToken.ORB) {
			List<TypeReference> trs = new ArrayList<>();
			parseInsideRB(tt.location, line, x -> trs.add(x));
			if (trs.isEmpty())
				return;
			curr = trs.get(0);
		} else {
			errors.message(tt.location, "invalid type reference");
			return;
		}
		
		int mark = line.at();
		TypeExprToken nt = TypeExprToken.from(errors, line);
		if (nt == null) { // we have reached the end of the line
			errors.logReduction("simple-type-name", tt, tt);
			pt.provide(curr);
			return;
		} else if (nt.type == TypeExprToken.CSB) {
			errors.logReduction("simple-type-name", tt, tt);
			line.reset(mark);
			pt.provide(curr);
			return;
		}

		// we may have polymorphic args
		if (nt.type == TypeExprToken.OSB) {
			TypeExprToken osb = nt;
			List<TypeReference> trs = new ArrayList<>();
			int cnt = 0;
			TypeExprToken comma = null, csb = null;
			while (true) {
				tryParsing(line, x -> trs.add(x));
				if (++cnt != trs.size()) { // we didn't get a type
					return;
				}
				if (comma != null) {
					errors.logReduction("comma-type-reference", comma, trs.get(trs.size()-1));
				}
				nt = TypeExprToken.from(errors, line); // get the next token
				// TODO: can probably be null
				if (nt.type == TypeExprToken.COMMA) {
					comma = nt;
				} else if (nt.type == TypeExprToken.CSB) {
					csb = nt;
					break;
				} else {
					// probably a real error
					throw new NotImplementedException();
				}
			}
			curr = new TypeReference(curr.location(), curr.name(), trs);
			errors.logReduction("poly-type-list", osb, csb);
			errors.logReduction("simple-type-name-with-polys", tt, csb);
			mark = line.at();
		}
		
		// now try a function
		if (nt.type == TypeExprToken.ARROW) {
			List<TypeReference> trs = new ArrayList<>();
			tryParsing(line, x -> trs.add(x));
			if (trs.isEmpty()) {
				return;
			} else {
				TypeReference restr = trs.get(0);
				List<TypeReference> args;
				if (restr instanceof FunctionTypeReference) {
					FunctionTypeReference resf = (FunctionTypeReference) restr;
					args = new ArrayList<TypeReference>(resf.args);
				} else {
					args = new ArrayList<TypeReference>();
					args.add(restr);
				}
				args.add(0, curr);
				curr = new FunctionTypeReference(curr.location(), args);
			}
		} else {
			line.reset(mark);
		}
	
		pt.provide(curr);
	}

	private void parseInsideRB(InputPosition orbLoc, Tokenizable line, TDAProvideType pt) {
		List<TypeReference> trs = new ArrayList<>();
		while (true) {
			tryParsing(line, x -> trs.add(x));
			if (trs.isEmpty()) {
				return;
			}
			TypeExprToken tt = TypeExprToken.from(errors, line);
			if (tt.type == TypeExprToken.CRB) {
				if (trs.size() == 1) // the parenthesis case
					pt.provide(trs.get(0));
				else { // the tuple case
					pt.provide(new TupleTypeReference(orbLoc, trs));
				}
				return;
			} else if (tt.type == TypeExprToken.COMMA) {
				; // loop for next type
			} else {
				// probably a real error
				throw new NotImplementedException();
			}
		}			
	}
}
