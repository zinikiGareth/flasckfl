package org.flasck.flas.parsedForm.ut;

public class MatchedItem {
	private final String item;

	public MatchedItem(String item) {
		this.item = item;
	}
	
	public String item() {
		return item;
	}
	
	@Override
	public int hashCode() {
		return "MatchedItem".hashCode() ^ item.hashCode();
	}
	
	@Override
	public boolean equals(Object obj) {
		return obj instanceof MatchedItem && item.equals(((MatchedItem)obj).item);
	}
}
