package test.flas.testrunner;

import org.flasck.flas.testrunner.AssertTestStep;
import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;

public class AssertMatcher extends TypeSafeMatcher<AssertTestStep> {
	private final Matcher<Object> eval;

	public AssertMatcher(Matcher<Object> eval) {
		this.eval = eval;
	}

	@Override
	public void describeTo(Description desc) {
		desc.appendText("is evaluating expr '");
		eval.describeTo(desc);
		desc.appendText("'");
	}

	@Override
	protected boolean matchesSafely(AssertTestStep in) {
		return eval.matches(in.eval);
	}
}
