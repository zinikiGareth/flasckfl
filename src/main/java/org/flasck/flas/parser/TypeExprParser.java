package org.flasck.flas.parser;

import java.util.ArrayList;
import java.util.List;

import org.flasck.flas.parsedForm.TypeReference;
import org.flasck.flas.tokenizers.Tokenizable;
import org.flasck.flas.tokenizers.TypeExprToken;
import org.zinutils.exceptions.UtilException;

public class TypeExprParser implements TryParsing {

	@Override
	public Object tryParsing(Tokenizable line) {
		TypeExprToken tt = TypeExprToken.from(line);
		if (tt == null)
			return null; // not even a valid token (or line ended)
		if (tt.type == TypeExprToken.NAME)
			return new TypeReference(tt.text);
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
			TypeReference tr = new TypeReference(next.text);
			add = tr;
			TypeExprToken look;
			mark = line.at();
			while (line.hasMore() && (look = TypeExprToken.from(line)) != null && look.type != TypeExprToken.CRB && look.type != TypeExprToken.COMMA) {
				line.reset(mark);
				Object ta = tryParsing(line);
				if (ta == null)
					return null; // error happened inside, return it
				tr.args.add(ta);
				mark = line.at();
			}
			line.reset(mark); // want to see the CRB/COMMA again
		}
		return add;
	}
}
