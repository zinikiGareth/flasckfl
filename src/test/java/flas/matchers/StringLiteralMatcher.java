package flas.matchers;

import org.flasck.flas.commonBase.StringLiteral;
import org.hamcrest.Description;
import org.hamcrest.TypeSafeMatcher;

public class StringLiteralMatcher extends TypeSafeMatcher<StringLiteral> {
	private String match;

	public StringLiteralMatcher(String literal) {
		match = literal;
	}

	@Override
	public void describeTo(Description desc) {
		desc.appendText("matches the string literal '" + match + "'");
	}

	@Override
	protected boolean matchesSafely(StringLiteral val) {
		return match.equals(val.text);
	}

}
