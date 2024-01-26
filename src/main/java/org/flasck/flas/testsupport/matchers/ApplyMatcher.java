package org.flasck.flas.testsupport.matchers;

import java.util.Arrays;
import java.util.List;

import org.flasck.flas.tc3.Apply;
import org.flasck.flas.tc3.Type;
import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;

public class ApplyMatcher extends TypeSafeMatcher<Apply> {

	private final List<Matcher<Type>> ts;

	private ApplyMatcher(Matcher<Type>[] ts) {
		this.ts = Arrays.asList(ts);
	}

	@Override
	public void describeTo(Description arg0) {
		arg0.appendText("Apply[");
		String sep = "";
		for (Matcher<Type> t : ts) {
			arg0.appendText(sep);
			t.describeTo(arg0);
			sep = "->";
		}
		arg0.appendText("]");
	}

	@Override
	protected boolean matchesSafely(Apply arg0) {
		if (arg0.argCount() != ts.size()-1)
			return false;
		for (int i=0;i<ts.size();i++) {
			if (!ts.get(i).matches(arg0.get(i)))
				return false;
		}
		return true;
	}

	public static ApplyMatcher type(@SuppressWarnings("unchecked") Matcher<Type>... ts) {
		return new ApplyMatcher(ts);
	}

}
