package flas.matchers;

import org.flasck.flas.parsedForm.CardDefinition;
import org.hamcrest.Description;
import org.hamcrest.TypeSafeMatcher;

public class CardDefnMatcher extends TypeSafeMatcher<CardDefinition> {
	private final String name;

	public CardDefnMatcher(String name) {
		this.name = name;
	}

	@Override
	public void describeTo(Description arg0) {
		arg0.appendText("Card(");
		arg0.appendValue(name);
		arg0.appendText(")");
	}

	@Override
	protected boolean matchesSafely(CardDefinition arg0) {
		if (!arg0.cardName.uniqueName().equals(name))
			return false;
		return true;
	}

	public static CardDefnMatcher called(String name) {
		return new CardDefnMatcher(name);
	}
}
