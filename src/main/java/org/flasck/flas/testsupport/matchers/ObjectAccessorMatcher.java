package org.flasck.flas.testsupport.matchers;

import org.flasck.flas.parsedForm.ObjectAccessor;
import org.hamcrest.Description;
import org.hamcrest.TypeSafeMatcher;

public class ObjectAccessorMatcher extends TypeSafeMatcher<ObjectAccessor> {
	private final FunctionDefinitionMatcher fn;

	private ObjectAccessorMatcher(FunctionDefinitionMatcher fn) {
		this.fn = fn;
	}

	@Override
	public void describeTo(Description arg0) {
		arg0.appendText("OAM{");
		fn.describeTo(arg0);
		arg0.appendText("}");
	}

	@Override
	protected boolean matchesSafely(ObjectAccessor oa) {
		return fn.matches(oa.function());
	}

	public static ObjectAccessorMatcher of(FunctionDefinitionMatcher fn) {
		return new ObjectAccessorMatcher(fn);
	}

}
