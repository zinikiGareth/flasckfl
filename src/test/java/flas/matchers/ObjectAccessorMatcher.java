package flas.matchers;

import org.flasck.flas.parsedForm.ObjectAccessor;
import org.hamcrest.Description;
import org.hamcrest.TypeSafeMatcher;

public class ObjectAccessorMatcher extends TypeSafeMatcher<ObjectAccessor> {
	private final FunctionCaseMatcher fn;

	private ObjectAccessorMatcher(FunctionCaseMatcher fn) {
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
		return true;
	}

	public static ObjectAccessorMatcher of(FunctionCaseMatcher fn) {
		return new ObjectAccessorMatcher(fn);
	}

}
