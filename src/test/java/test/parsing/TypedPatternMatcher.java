package test.parsing;

import org.flasck.flas.commonBase.Pattern;
import org.flasck.flas.parsedForm.TypedPattern;
import org.flasck.flas.parsedForm.VarPattern;
import org.hamcrest.Description;
import org.hamcrest.TypeSafeMatcher;

public class TypedPatternMatcher extends TypeSafeMatcher<Pattern> {
	private final String type;
	private final String var;

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
			if (var.equals(patt.var) && type.equals(patt.type.name()))
				return true;
		}
		return false;
	}

	public static TypedPatternMatcher typed(String type, String var) {
		return new TypedPatternMatcher(type, var);
	}
}
