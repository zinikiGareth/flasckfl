package flas.matchers;

import org.flasck.flas.tokenizers.TemplateNameToken;
import org.hamcrest.Description;
import org.hamcrest.TypeSafeMatcher;

public class TemplateNameTokenMatcher extends TypeSafeMatcher<TemplateNameToken> {
	private final String name;

	private TemplateNameTokenMatcher(String name) {
		this.name = name;
	}

	@Override
	public void describeTo(Description desc) {
		desc.appendText("templateName '" + name + "'");
	}

	@Override
	protected boolean matchesSafely(TemplateNameToken tok) {
		return tok.text.equals(name);
	}

	public static TemplateNameTokenMatcher named(final String name) {
		return new TemplateNameTokenMatcher(name);
	}
}
