package org.flasck.flas.parsedForm;

import org.flasck.flas.blockForm.InputPosition;
import org.zinutils.exceptions.UtilException;

public class CardMember implements ExternalRef {
	public final InputPosition location;
	public final String card;
	public final String var;

	public CardMember(InputPosition location, String card, String var) {
		this.location = location;
		this.card = card;
		this.var = var;
	}
	
	@Override
	public InputPosition location() {
		return location;
	}

	@Override
	public int compareTo(Object o) {
		return this.toString().compareTo(o.toString());
	}

	public String uniqueName() {
		return card +"."+var;
	}
	
	public boolean fromHandler() {
		throw new UtilException("This is not available");
	}
	
	@Override
	public String toString() {
		return "CardMember[" + uniqueName() + "]";
	}

}
