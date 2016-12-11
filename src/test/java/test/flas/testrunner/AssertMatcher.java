package test.flas.testrunner;

import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.testrunner.AssertTestStep;
import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;

public class AssertMatcher extends TypeSafeMatcher<AssertTestStep> {
	private final Matcher<Object> eval;
	private final Matcher<Object> expected;
	private InputPosition evalLocation;
	private InputPosition valueLocation;

	public AssertMatcher(Matcher<Object> eval, Matcher<Object> expected) {
		this.eval = eval;
		this.expected = expected;
	}
	
	public AssertMatcher evalLocation(String file, int line, int pos, String text) {
		evalLocation = new InputPosition(file, line, pos, text);
		return this;
	}

	public AssertMatcher valueLocation(String file, int line, int pos, String text) {
		valueLocation = new InputPosition(file, line, pos, text);
		return this;
	}

	@Override
	public void describeTo(Description desc) {
		desc.appendText("is evaluating expr '");
		eval.describeTo(desc);
		desc.appendText("' and expecting '");
		expected.describeTo(desc);
		desc.appendText("'");
		if (evalLocation != null)
			desc.appendText(" and need eval location to be " + evalLocation);
		if (valueLocation != null)
			desc.appendText(" and need eval location to be " + valueLocation);
	}

	@Override
	protected boolean matchesSafely(AssertTestStep in) {
		if (!eval.matches(in.eval))
			return false;
		if (evalLocation != null && !evalLocation.equals(in.evalPos))
			return false;
		if (valueLocation != null && !valueLocation.equals(in.evalPos))
			return false;
		return true;
	}
}
