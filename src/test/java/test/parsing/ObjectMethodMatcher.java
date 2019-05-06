package test.parsing;

import org.flasck.flas.parsedForm.ObjectMethod;
import org.hamcrest.Description;
import org.hamcrest.TypeSafeMatcher;

public class ObjectMethodMatcher extends TypeSafeMatcher<ObjectMethod> {

	@Override
	public void describeTo(Description desc) {
		desc.appendText("ObjectMethod");
	}

	@Override
	protected boolean matchesSafely(ObjectMethod arg0) {
		return true;
	}

	public static ObjectMethodMatcher called(Object object, String string) {
		return new ObjectMethodMatcher();
	}

}
