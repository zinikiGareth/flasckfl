package org.flasck.flas.parser;

import java.util.ArrayList;
import java.util.List;

import org.flasck.flas.parsedForm.UnionTypeDefn;
import org.flasck.flas.tokenizers.KeywordToken;
import org.flasck.flas.tokenizers.PeekToken;
import org.flasck.flas.tokenizers.Tokenizable;
import org.flasck.flas.tokenizers.TypeNameToken;
import org.flasck.flas.typechecker.Type;

public class TypeDefnParser implements TryParsing {

	@Override
	public Object tryParsing(Tokenizable line) {
		KeywordToken kw = KeywordToken.from(line);
		if (!"type".equals(kw.text))
			return null;
		TypeNameToken tn = TypeNameToken.from(line);
		if (tn == null)
			return null; // invalid type name

		List<Type> args = new ArrayList<Type>();
		while (line.hasMore() && !PeekToken.is(line, "=")) {
			TypeNameToken ta = TypeNameToken.from(line);
			if (ta == null)
				return null; // invalid type argument
			args.add(Type.polyvar(ta.location, ta.text));
		}
		if (!PeekToken.accept(line, "="))
			return null;
		UnionTypeDefn ret = new UnionTypeDefn(line.realinfo(), true, tn.text, args);
		while (line.hasMore()) {
			Object tr = new TypeExprParser().tryOneExpr(line);
			if (tr == null)
				return null; // invalid type argument
			else
				ret.cases.add((Type) tr);
			if (line.hasMore())
				PeekToken.accept(line, "|");
		}
		return ret;
	}

}
