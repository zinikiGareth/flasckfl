package test.tc3;

import org.flasck.flas.tc3.FunctionChecker.ArgResult;
import org.flasck.flas.tc3.Type;
import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;

public class ArgResultMatcher extends TypeSafeMatcher<ArgResult> {
	private final Matcher<Type> ty;

	private ArgResultMatcher(Matcher<Type> ty) {
		this.ty = ty;
	}

	@Override
	public void describeTo(Description arg0) {
		arg0.appendText("ArgResult[");
		arg0.appendValue(ty);
		arg0.appendText("]");
	}

	@Override
	protected boolean matchesSafely(ArgResult arg0) {
		if (!ty.matches(arg0.type))
			return false;
		return true;
	}

	public static ArgResultMatcher expr(Matcher<Type> ty) {
		return new ArgResultMatcher(ty);
	}

}
