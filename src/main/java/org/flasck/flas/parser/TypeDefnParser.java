package org.flasck.flas.parser;

import java.util.ArrayList;
import java.util.List;

import org.flasck.flas.commonBase.names.SolidName;
import org.flasck.flas.parsedForm.PolyType;
import org.flasck.flas.parsedForm.TypeReference;
import org.flasck.flas.parsedForm.UnionTypeDefn;
import org.flasck.flas.stories.FLASStory.State;
import org.flasck.flas.tokenizers.KeywordToken;
import org.flasck.flas.tokenizers.PeekToken;
import org.flasck.flas.tokenizers.PolyTypeToken;
import org.flasck.flas.tokenizers.Tokenizable;
import org.flasck.flas.tokenizers.TypeNameToken;

public class TypeDefnParser implements TryParsing {
	private final State state;

	public TypeDefnParser(State state) {
		this.state = state;
	}
	
	@Override
	public Object tryParsing(Tokenizable line) {
		KeywordToken kw = KeywordToken.from(line);
		if (!"type".equals(kw.text))
			return null;
		TypeNameToken tn = TypeNameToken.unqualified(line);
		if (tn == null)
			return null; // invalid type name

		List<PolyType> args = new ArrayList<PolyType>();
		while (line.hasMore() && !PeekToken.is(line, "=")) {
			PolyTypeToken ta = PolyTypeToken.from(line);
			if (ta == null)
				return null; // invalid type argument
			args.add(new PolyType(ta.location, ta.text));
		}
		if (!PeekToken.accept(line, "="))
			return null;
		UnionTypeDefn ret = new UnionTypeDefn(line.realinfo(), true, new SolidName(state.pkgName, tn.text), args);
		while (line.hasMore()) {
			Object tr = new TypeExprParser().tryOneExpr(line);
			if (tr == null)
				return null; // invalid type argument
			else
				ret.cases.add((TypeReference) tr);
			if (line.hasMore())
				PeekToken.accept(line, "|");
		}
		return ret;
	}

}
