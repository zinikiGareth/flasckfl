package org.flasck.flas.parser;

import java.util.ArrayList;
import java.util.List;

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
		TypeExprToken tt = TypeExprToken.from(line);
		if (tt == null)
			return; // not even a valid token (or line ended)
		TypeReference curr = null;
		if (tt.type == TypeExprToken.NAME) {
			curr = new TypeReference(tt.location, tt.text);
		} else if (tt.type == TypeExprToken.ORB) {
			List<TypeReference> trs = new ArrayList<>();
			parseInsideRB(line, x -> trs.add(x));
			if (trs.isEmpty())
				return;
			curr = trs.get(0);
		} else {
			errors.message(tt.location, "invalid type reference");
//			throw new NotImplementedException("handle ORB at least");
			return;
		}
		
		int mark = line.at();
		TypeExprToken nt = TypeExprToken.from(line);
		if (nt == null) { // we have reached the end of the line
			pt.provide(curr);
			return;
		}

		// polys go here
		
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
	
			/*
			List<TypeReference> polys = new ArrayList<TypeReference>();
			int mark = line.at();
			TypeExprToken osb = TypeExprToken.from(line);
			if (osb != null && osb.type == TypeExprToken.OSB) {
				while (line.hasMoreContent()) {
					tryParsing(line, tmp -> polys.add(tmp));
					osb = TypeExprToken.from(line);
					if (osb.type == TypeExprToken.CSB) {
						break;
					}
					else if (osb.type != TypeExprToken.COMMA)
						return;
				}
			} else
				line.reset(mark);
			List<TypeReference> fnargs = new ArrayList<TypeReference>();
			fnargs.add(new TypeReference(tt.location, tt.text, polys));
			InputPosition arrow = null;
			while (true) {
				mark = line.at();
				TypeExprToken arr = TypeExprToken.from(line);
				if (arr != null && arr.type == TypeExprToken.ARROW) {
					arrow = arr.location;
					tryOneExpr(line, tr -> fnargs.add((TypeReference) tr) );
				} else {
					line.reset(mark);
					break;
				}
			}
			// The normal case, where we just have one type
			if (fnargs.size() == 1) {
				pt.provide(fnargs.get(0));
			} else {
				// This is a function type, such as "A->B mapper"
				pt.provide(new FunctionTypeReference(arrow, fnargs));
				return;
			}
		}
		else if (tt.type == TypeExprToken.ORB) {
			// either a complex type, grouped OR a tuple type
			// Start parsing nested expression and see what happens
			int cnt = 0;
			List<TypeReference> inner = new ArrayList<>();
			while (line.hasMoreContent()) {
				tryOneExpr(line, add -> inner.add((TypeReference) add) );
				if (++cnt != inner.size())
					return;
				TypeExprToken crb = TypeExprToken.from(line);
				if (crb.type == TypeExprToken.CRB) {
					if (inner.size() == 1) {
						pt.provide(inner.get(0));
						return;
					} else if (inner.size() > 1) {
						pt.provide(new TupleTypeReference(tt.location, inner));
						return;
					}
				} else if (crb.type != TypeExprToken.COMMA)
					return; // this is an error
			}
		} 
		// not a valid type expression
		 */
		
		pt.provide(curr);
	}

	private void parseInsideRB(Tokenizable line, TDAProvideType pt) {
		List<TypeReference> trs = new ArrayList<>();
		while (true) {
			tryParsing(line, x -> trs.add(x));
			if (trs.isEmpty()) {
				return;
			}
			TypeExprToken tt = TypeExprToken.from(line);
			if (tt.type == TypeExprToken.CRB) {
				if (trs.size() == 1) // the parenthesis case
					pt.provide(trs.get(0));
				else { // the tuple case
					pt.provide(new TupleTypeReference(trs.get(0).location(), trs));
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

	/*
	public void tryOneExpr(Tokenizable line, TDAProvideType pt) {
		int mark = line.at();
		TypeExprToken next = TypeExprToken.from(line);
		if (next == null)
			return; // some kind of error - EOF? invalid token?
		else if (next.type == TypeExprToken.ORB) {
			// it's a complex nested type; push this back and call ourselves recursively
			line.reset(mark);
			tryParsing(line, pt);
		} else if (next.type == TypeExprToken.NAME) {
			// it's a function application of types
			TypeExprToken look;
			mark = line.at();
			int cnt = 0;
			List<TypeReference> args = new ArrayList<TypeReference>();
			while (line.hasMoreContent() && (look = TypeExprToken.from(line)) != null && look.type == TypeExprToken.ARROW) {
				tryOneExpr(line, t -> args.add(t));
				if (++cnt != args.size())
					return; // there was an error
				mark = line.at(); // update mark
			}
			TypeReference tr = new TypeReference(next.location, next.text, args);
			line.reset(mark); // want to see the CRB/COMMA again
			pt.provide(tr);
		}
	}
	*/
}
