package org.flasck.flas.testsupport.matchers;

import org.flasck.flas.commonBase.Pattern;
import org.flasck.flas.parsedForm.VarPattern;
import org.hamcrest.Description;

public class VarPatternMatcher extends PatternMatcher {
	private final String var;

	public VarPatternMatcher(String var) {
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
			if (var.equals(((VarPattern)arg0).name().uniqueName()))
				return true;
		}
		return false;
	}
}
