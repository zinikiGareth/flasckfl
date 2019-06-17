package test.parsing;

import org.flasck.flas.parsedForm.HandlerImplements;
import org.hamcrest.Description;
import org.hamcrest.TypeSafeMatcher;

public class HandlerImplementsMatcher extends TypeSafeMatcher<HandlerImplements> {
	private final String name;

	public HandlerImplementsMatcher(String name) {
		this.name = name;
	}

	@Override
	public void describeTo(Description desc) {
		desc.appendText("HandlerImplements[");
		desc.appendValue(name);
		desc.appendText("]");
	}

	@Override
	protected boolean matchesSafely(HandlerImplements hi) {
		if (hi.handlerName.uniqueName().equals(name))
			return true;
		return false;
	}

	public static HandlerImplementsMatcher named(String name) {
		return new HandlerImplementsMatcher(name);
	}

}
