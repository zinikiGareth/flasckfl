package org.flasck.flas.parser;

import java.util.ArrayList;
import java.util.List;

import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.parsedForm.FunctionTypeReference;
import org.flasck.flas.parsedForm.TupleTypeReference;
import org.flasck.flas.parsedForm.TypeReference;
import org.flasck.flas.tokenizers.Tokenizable;
import org.flasck.flas.tokenizers.TypeExprToken;

// TODO: this isn't actually TDA ...
public class TypeExprParser {
	public Object tryParsing(Tokenizable line) {
		TypeExprToken tt = TypeExprToken.from(line);
		if (tt == null)
			return null; // not even a valid token (or line ended)
		if (tt.type == TypeExprToken.NAME) {
			List<TypeReference> polys = new ArrayList<TypeReference>();
			int mark = line.at();
			TypeExprToken osb = TypeExprToken.from(line);
			if (osb != null && osb.type == TypeExprToken.OSB) {
				while (line.hasMoreContent()) {
					TypeReference tmp = (TypeReference) tryParsing(line);
					if (tmp == null)
						return null;
					polys.add(tmp);
					osb = TypeExprToken.from(line);
					if (osb.type == TypeExprToken.CSB) {
						break;
					}
					else if (osb.type != TypeExprToken.COMMA)
						return null;
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
					Object t = tryOneExpr(line);
					if (t instanceof TypeReference)
						fnargs.add((TypeReference) t);
					else
						return t;
				} else {
					line.reset(mark);
					break;
				}
			}
			// The normal case, where we just have one type
			if (fnargs.size() == 1)
				return fnargs.get(0);
			else {
				// This is a function type, such as "A->B mapper"
				return new FunctionTypeReference(arrow, fnargs);
			}
		}
		else if (tt.type == TypeExprToken.ORB) {
			// either a complex type, grouped OR a tuple type
			// Start parsing nested expression and see what happens
			List<TypeReference> inner = new ArrayList<>();
			while (line.hasMoreContent()) {
				Object add = tryOneExpr(line);
				if (add == null)
					return null; // and issue an error
				inner.add((TypeReference) add);
				TypeExprToken crb = TypeExprToken.from(line);
				if (crb.type == TypeExprToken.CRB) {
					if (inner.size() == 1)
						return inner.get(0);
					else if (inner.size() > 1)
						return new TupleTypeReference(tt.location, inner);
				} else if (crb.type != TypeExprToken.COMMA)
					return null; // this is an error
			}
			return null; // unexpected EOF
		} else
			return null; // not a valid type expression
	}

	public Object tryOneExpr(Tokenizable line) {
		Object add = null;
		int mark = line.at();
		TypeExprToken next = TypeExprToken.from(line);
		if (next == null)
			return null; // some kind of error - EOF? invalid token?
		else if (next.type == TypeExprToken.ORB) {
			// it's a complex nested type; push this back and call ourselves recursively
			line.reset(mark);
			add = tryParsing(line);
		} else if (next.type == TypeExprToken.NAME) {
			// it's a function application of types
			TypeExprToken look;
			mark = line.at();
			List<TypeReference> args = new ArrayList<TypeReference>();
			while (line.hasMoreContent() && (look = TypeExprToken.from(line)) != null && look.type != TypeExprToken.CRB && look.type != TypeExprToken.CSB && look.type != TypeExprToken.COMMA) {
				line.reset(mark);
				Object ta = tryParsing(line);
				if (ta == null)
					return null; // error happened inside, return it
				args.add((TypeReference) ta);
				mark = line.at();
			}
			TypeReference tr = new TypeReference(next.location, next.text, args);
			add = tr;
			line.reset(mark); // want to see the CRB/COMMA again
		}
		return add;
	}
}
