package org.flasck.flas.testsupport.matchers;

import org.flasck.flas.tokenizers.Tokenizable;
import org.hamcrest.Description;
import org.hamcrest.TypeSafeMatcher;

public class TokenizableMatcher extends TypeSafeMatcher<Tokenizable> {

	private final String text;

	public TokenizableMatcher(String text) {
		this.text = text;
	}

	@Override
	public void describeTo(Description arg0) {
		arg0.appendText("A tokenized string with text");
		arg0.appendValue(text);
	}

	@Override
	protected boolean matchesSafely(Tokenizable arg0) {
		if (arg0 == null)
			return false;
		return text.equals(arg0.getTo(arg0.length()));
	}

	public static TokenizableMatcher match(String string) {
		return new TokenizableMatcher(string);
	}

}
