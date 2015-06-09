package org.flasck.flas.parsedForm;

public class CardMember implements ExternalRef {
	public final String card;
	public final String var;

	public CardMember(String card, String var) {
		this.card = card;
		this.var = var;
	}
	
	@Override
	public int compareTo(Object o) {
		return this.toString().compareTo(o.toString());
	}

	public String uniqueName() {
		return card +"."+var;
	}
	
	@Override
	public String toString() {
		return "Card[" + uniqueName() + "]";
	}

}
