package test.flas.testrunner;

import org.flasck.flas.commonBase.ApplyExpr;
import org.flasck.flas.commonBase.NumericLiteral;
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

	public static Matcher<Object> number(final Integer k) {
		return new TypeSafeMatcher<Object>() {
			@Override
			public void describeTo(Description desc) {
				desc.appendText("is numeric literal '" + k + "'");
			}

			@Override
			protected boolean matchesSafely(Object expr) {
				return expr instanceof NumericLiteral && ((NumericLiteral)expr).text.equals(Integer.toString(k));
			}
		};
	}

	public static Matcher<Object> apply(Matcher<Object> fn, final Matcher<Object>... args) {
		return new TypeSafeMatcher<Object>() {
			@Override
			public void describeTo(Description desc) {
				desc.appendText("is apply of ");
				fn.describeTo(desc);
				desc.appendText(" to");
				for (Matcher<Object> m : args) {
					desc.appendText(" ");
					m.describeTo(desc);
				}
			}

			@Override
			protected boolean matchesSafely(Object expr) {
				if (!(expr instanceof ApplyExpr))
					return false;
				ApplyExpr ae = (ApplyExpr) expr;
				if (!fn.matches(ae.fn) || ae.args.size() != args.length)
					return false;
				for (int i=0;i<args.length;i++) {
					if (!args[i].matches(ae.args.get(i)))
						return false;
				}
				return true;
			}
		};
	}
}
