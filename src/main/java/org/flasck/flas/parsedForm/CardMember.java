package org.flasck.flas.parsedForm;

import java.io.Serializable;

import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.typechecker.Type;
import org.zinutils.exceptions.UtilException;

@SuppressWarnings("serial")
public class CardMember implements ExternalRef, Serializable {
	public final InputPosition location;
	public final String card;
	public final String var;
	public final Type type;

	public CardMember(InputPosition location, String card, String var, Type type) {
		this.location = location;
		this.card = card;
		this.var = var;
		this.type = type;
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
