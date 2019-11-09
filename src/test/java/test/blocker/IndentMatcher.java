package test.blocker;

import org.flasck.flas.blockForm.Indent;
import org.hamcrest.Description;
import org.hamcrest.TypeSafeMatcher;

public class IndentMatcher extends TypeSafeMatcher<Indent> {
	private final int tabs;
	private final int spaces;

	public IndentMatcher(int tabs, int spaces) {
		this.tabs = tabs;
		this.spaces = spaces;
	}

	@Override
	public void describeTo(Description arg0) {
		arg0.appendText("Yay");
	}

	@Override
	protected boolean matchesSafely(Indent arg0) {
		return arg0.tabs == tabs && arg0.spaces == spaces;
	}

	public static IndentMatcher match(int tabs, int spaces) {
		return new IndentMatcher(tabs, spaces);
	}

}
