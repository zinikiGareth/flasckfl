package org.flasck.flas.testsupport.matchers;

import org.flasck.flas.parsedForm.IntroduceVar;
import org.hamcrest.Description;
import org.hamcrest.TypeSafeMatcher;

public class IntroduceVarMatcher extends TypeSafeMatcher<IntroduceVar> {

	private final String called;

	public IntroduceVarMatcher(String called) {
		this.called = called;
	}

	@Override
	public void describeTo(Description arg0) {
		arg0.appendText("introducedVar(_");
		arg0.appendValue(called);
		arg0.appendText(")");
	}

	@Override
	protected boolean matchesSafely(IntroduceVar arg0) {
		return called.equals(arg0.var);
	}

	public static IntroduceVarMatcher called(String string) {
		return new IntroduceVarMatcher(string);
	}

}
