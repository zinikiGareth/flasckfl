package test.parsing;

import org.flasck.flas.commonBase.Pattern;
import org.flasck.flas.parsedForm.VarPattern;
import org.hamcrest.Description;
import org.hamcrest.TypeSafeMatcher;

public class PatternMatcher extends TypeSafeMatcher<Pattern> {
	private final String var;

	public PatternMatcher(String var) {
		this.var = var;
	}

	@Override
	public void describeTo(Description arg0) {
		arg0.appendText("Var<");
		arg0.appendValue(var);
		arg0.appendText(">");
	}

	@Override
	protected boolean matchesSafely(Pattern arg0) {
		if (arg0 instanceof VarPattern) {
			if (((VarPattern)arg0).var.equals(var))
				return true;
		}
		return false;
	}

	public static PatternMatcher var(String var) {
		return new PatternMatcher(var);
	}
}
