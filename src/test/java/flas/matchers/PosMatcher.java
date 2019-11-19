package flas.matchers;

import org.flasck.flas.tc3.PosType;
import org.flasck.flas.tc3.Type;
import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;

public class PosMatcher extends TypeSafeMatcher<PosType> {
	private final Matcher<Type> type;

	public PosMatcher(Matcher<Type> type) {
		this.type = type;
	}

	@Override
	public void describeTo(Description arg0) {
		arg0.appendText("PosMatcher[");
		type.describeTo(arg0);
		arg0.appendText("]");
	}

	@Override
	protected boolean matchesSafely(PosType arg0) {
		return type.matches(arg0.type);
	}

	public static PosMatcher type(Matcher<Type> type) {
		return new PosMatcher(type);
	}

}
