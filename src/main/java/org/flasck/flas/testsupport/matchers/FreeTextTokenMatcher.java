package org.flasck.flas.testsupport.matchers;

import org.flasck.flas.tokenizers.FreeTextToken;
import org.hamcrest.Description;
import org.hamcrest.TypeSafeMatcher;

public class FreeTextTokenMatcher extends TypeSafeMatcher<FreeTextToken> {
	private final String text;

	public FreeTextTokenMatcher(String text) {
		this.text = text;
	}

	@Override
	public void describeTo(Description arg0) {
		arg0.appendText("A free text token with '");
		arg0.appendValue(text);
		arg0.appendText("'");
	}

	@Override
	protected boolean matchesSafely(FreeTextToken arg0) {
		String var = arg0.text();
		return var.equals(text);
	}


	public static FreeTextTokenMatcher text(String s) {
		return new FreeTextTokenMatcher(s);
	}
}
