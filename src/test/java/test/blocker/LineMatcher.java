package test.blocker;

import org.flasck.flas.blockForm.ContinuedLine;
import org.hamcrest.Description;
import org.hamcrest.TypeSafeMatcher;

public class LineMatcher extends TypeSafeMatcher<ContinuedLine> {
	private final String text;

	public LineMatcher(String text) {
		this.text = text;
	}

	@Override
	public void describeTo(Description arg0) {
		arg0.appendText("A line with text");
		arg0.appendValue(text);
	}

	@Override
	protected boolean matchesSafely(ContinuedLine arg0) {
		String var = arg0.text().toString();
		return var.toString().equals(text);
	}


	public static LineMatcher match(String string) {
		return new LineMatcher(string);
	}

}
