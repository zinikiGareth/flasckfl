package flas.matchers;

import org.flasck.flas.tc3.Type;
import org.flasck.flas.tc3.UnifiableType;
import org.hamcrest.Description;
import org.hamcrest.TypeSafeMatcher;

public class ResolvedUTMatcher extends TypeSafeMatcher<Type> {
	private final Type type;

	public ResolvedUTMatcher(Type type) {
		this.type = type;
	}

	@Override
	public void describeTo(Description arg0) {
		arg0.appendText("UT Matching[");
		arg0.appendValue(type);
		arg0.appendText("]");
	}

	@Override
	protected boolean matchesSafely(Type arg0) {
		if (!(arg0 instanceof UnifiableType))
			return false;
		UnifiableType ut = (UnifiableType) arg0;
		if (!ut.isResolved())
			return false;
		return ut.resolve(null, true).equals(type);
	}

	public static ResolvedUTMatcher with(Type type) {
		return new ResolvedUTMatcher(type);
	}

}
