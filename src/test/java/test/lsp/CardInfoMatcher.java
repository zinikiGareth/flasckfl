package test.lsp;

import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;

import org.hamcrest.Description;
import org.hamcrest.TypeSafeMatcher;
import org.zinutils.exceptions.NotImplementedException;

import com.google.gson.JsonObject;

public class CardInfoMatcher extends TypeSafeMatcher<JsonObject> {
	private String uri;
	private Map<String, JsonObject> expectCards = new TreeMap<>();

	public CardInfoMatcher(String uri) {
		this.uri = uri;
	}

	@Override
	public void describeTo(Description desc) {
		desc.appendText("CardInfo[");
		desc.appendValue(uri);
		desc.appendText(",cards:");
		desc.appendValue(expectCards);
		desc.appendText("]");
	}

	@Override
	protected boolean matchesSafely(JsonObject jo) {
		try {
			String asUri = jo.get("uri").getAsString();
			if (!uri.equals(asUri)) {
				return false;
			}
			JsonObject cards = jo.get("cards").getAsJsonObject();
			if (cards == null) {
				return false;
			}
			if (cards.size() != this.expectCards.size()) {
				return false;
			}
			for (Entry<String, JsonObject> e : this.expectCards.entrySet()) {
				JsonObject card = cards.get(e.getKey()).getAsJsonObject();
				if (card == null) {
					return false;
				}
				JsonObject expected = e.getValue();
				if (card.size() != expected.size()) {
					return false;
				}
				for (String qq : expected.keySet()) {
					throw new NotImplementedException("need to figure out looking at card fields: " + qq);
				}
			}
			return true;
		} catch (Exception ex) {
			return false;
		}
	}
	
	public CardInfoMatcher info(String forCard, JsonObject items) {
		expectCards.put(forCard, items);
		return this;
	}

	public static CardInfoMatcher ui(String uri) {
		return new CardInfoMatcher(uri);
	}
}
