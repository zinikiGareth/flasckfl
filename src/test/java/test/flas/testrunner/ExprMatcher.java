package test.flas.testrunner;

import org.flasck.flas.parsedForm.UnresolvedVar;
import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;

public class ExprMatcher {
	public static Matcher<Object> unresolved(final String name) {
		return new TypeSafeMatcher<Object>() {
			@Override
			public void describeTo(Description desc) {
				desc.appendText("is unresolved var '" + name + "'");
			}

			@Override
			protected boolean matchesSafely(Object expr) {
				return expr instanceof UnresolvedVar && ((UnresolvedVar)expr).var.equals(name);
			}
		};
	}
}
