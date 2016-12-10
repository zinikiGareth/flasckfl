package test.flas.testrunner;

import org.flasck.flas.testrunner.ValueTestCase;
import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;

public class ValueMatcher extends TypeSafeMatcher<ValueTestCase> {
	private final Matcher<Object> eval;

	public ValueMatcher(Matcher<Object> eval) {
		this.eval = eval;
	}

	@Override
	public void describeTo(Description desc) {
		desc.appendText("is evaluating expr '");
		eval.describeTo(desc);
		desc.appendText("'");
	}

	@Override
	protected boolean matchesSafely(ValueTestCase in) {
		return eval.matches(in.eval);
	}
}
