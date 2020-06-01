package flas.matchers;

import org.flasck.flas.tc3.EnsureListMessage;
import org.flasck.flas.tc3.Type;
import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;

public class ElmMatcher extends TypeSafeMatcher<Type> {
	private final Matcher<Type> ty;

	public ElmMatcher(Matcher<Type> ty) {
		this.ty = ty;
	}
	
	@Override
	public void describeTo(Description arg0) {
		arg0.appendText("EnsureListMessage[");
		arg0.appendValue(ty);
		arg0.appendText("]");
	}

	@Override
	protected boolean matchesSafely(Type arg0) {
		if (!(arg0 instanceof EnsureListMessage))
			return false;
		EnsureListMessage elm = (EnsureListMessage) arg0;
		if (!ty.matches(elm.checking()))
			return false;
		return true;
	}
}
