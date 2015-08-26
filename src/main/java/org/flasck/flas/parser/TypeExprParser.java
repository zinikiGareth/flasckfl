package org.flasck.flas.parser;

import java.util.ArrayList;
import java.util.List;

import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.tokenizers.Tokenizable;
import org.flasck.flas.tokenizers.TypeExprToken;
import org.flasck.flas.typechecker.Type;
import org.zinutils.exceptions.UtilException;

public class TypeExprParser implements TryParsing {

	@Override
	public Object tryParsing(Tokenizable line) {
		TypeExprToken tt = TypeExprToken.from(line);
		if (tt == null)
			return null; // not even a valid token (or line ended)
		if (tt.type == TypeExprToken.NAME) {
			List<Type> polys = new ArrayList<Type>();
			int mark = line.at();
			TypeExprToken osb = TypeExprToken.from(line);
			if (osb != null && osb.type == TypeExprToken.OSB) {
				while (line.hasMore()) {
					Type tmp = (Type) tryParsing(line);
					if (tmp == null)
						return null;
					polys.add(tmp);
					osb = TypeExprToken.from(line);
					if (osb.type == TypeExprToken.CSB)
						break;
					else if (osb.type != TypeExprToken.COMMA)
						return null;
				}
			} else
				line.reset(mark);
			return Type.reference(tt.location, tt.text, polys);
		}
		else if (tt.type == TypeExprToken.ORB) {
			// either a complex type, grouped OR a tuple type
			// Start parsing nested expression and see what happens
			List<Object> inner = new ArrayList<Object>();
			while (line.hasMore()) {
				Object add = tryOneExpr(line);
				if (add == null)
					return null; // and issue an error
				inner.add(add);
				TypeExprToken crb = TypeExprToken.from(line);
				if (crb.type == TypeExprToken.CRB) {
					if (inner.size() == 1)
						return inner.get(0);
					else if (inner.size() > 1)
						throw new UtilException("Tuple case");
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
			List<Type> args = new ArrayList<Type>();
			while (line.hasMore() && (look = TypeExprToken.from(line)) != null && look.type != TypeExprToken.CRB && look.type != TypeExprToken.COMMA) {
				line.reset(mark);
				Object ta = tryParsing(line);
				if (ta == null)
					return null; // error happened inside, return it
				args.add((Type) ta);
				mark = line.at();
			}
			Type tr = Type.reference(next.location, next.text, args);
			add = tr;
			line.reset(mark); // want to see the CRB/COMMA again
		}
		return add;
	}
}
