package flas.matchers;

import java.util.ArrayList;
import java.util.List;

import org.flasck.flas.commonBase.Pattern;
import org.flasck.flas.parsedForm.TypedPattern;
import org.hamcrest.Description;

public class TypedPatternMatcher extends PatternMatcher {
	private final String type;
	private final String var;
	private final List<String> typevars = new ArrayList<>();

	public TypedPatternMatcher(String type, String var) {
		this.type = type;
		this.var = var;
	}

	@Override
	public void describeTo(Description arg0) {
		arg0.appendText("Typed<");
		arg0.appendValue(type);
		arg0.appendText(":");
		arg0.appendValue(var);
		arg0.appendText(">");
	}

	@Override
	protected boolean matchesSafely(Pattern arg0) {
		if (arg0 instanceof TypedPattern) {
			final TypedPattern patt = (TypedPattern)arg0;
			if (!var.equals(patt.var.var) || !type.equals(patt.type.name()))
				return false;
			if (typevars.size() != patt.type.polys().size())
				return false;
			for (int i=0;i<typevars.size();i++) {
				if (!typevars.get(i).equals(patt.type.polys().get(i).name()))
					return false;
			}
		}
		return true;
	}

	public TypedPatternMatcher typevar(String tv) {
		typevars.add(tv);
		return this;
	}
}
