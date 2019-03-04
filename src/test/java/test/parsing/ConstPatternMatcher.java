package test.parsing;

import org.flasck.flas.commonBase.ConstPattern;
import org.flasck.flas.commonBase.Pattern;
import org.hamcrest.Description;
import org.hamcrest.TypeSafeMatcher;

public class ConstPatternMatcher extends TypeSafeMatcher<Pattern> {
	private final Integer n;
	private final Boolean b;

	public ConstPatternMatcher(int n) {
		this.n = n;
		this.b = null;
	}

	public ConstPatternMatcher(boolean b) {
		this.n = null;
		this.b = b;
	}

	@Override
	public void describeTo(Description arg0) {
		if (n != null) {
			arg0.appendText("numericPattern");
			arg0.appendValue(n);
		}
		if (b != null) {
			arg0.appendText("boolPattern");
			arg0.appendValue(b);
		}
	}

	@Override
	protected boolean matchesSafely(Pattern arg0) {
		if (!(arg0 instanceof ConstPattern))
			return false;
		ConstPattern cp = (ConstPattern) arg0;
		if (n != null) {
			if (cp.type != ConstPattern.INTEGER)
				return false;
			return cp.value.equals(Integer.toString(n));
		}
		if (b != null) {
			if (cp.type != ConstPattern.BOOLEAN)
				return false;
			return cp.value.equals(Boolean.toString(b));
		}
		return false;
	}

	public static ConstPatternMatcher number(int n) {
		return new ConstPatternMatcher(n);
	}

	public static ConstPatternMatcher truth(boolean b) {
		return new ConstPatternMatcher(b);
	}
}
