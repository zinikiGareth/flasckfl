package test.tc3;

import org.flasck.flas.tc3.ExpressionChecker.ExprResult;
import org.flasck.flas.tc3.Type;
import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;

public class ExprResultMatcher extends TypeSafeMatcher<ExprResult> {
	private final Matcher<Type> ty;

	private ExprResultMatcher(Matcher<Type> ty) {
		this.ty = ty;
	}

	@Override
	public void describeTo(Description arg0) {
		arg0.appendText("ExprResult[");
		arg0.appendValue(ty);
		arg0.appendText("]");
	}

	@Override
	protected boolean matchesSafely(ExprResult arg0) {
		if (!ty.matches(arg0.type))
			return false;
		return true;
	}

	public static ExprResultMatcher expr(Matcher<Type> ty) {
		return new ExprResultMatcher(ty);
	}

}
