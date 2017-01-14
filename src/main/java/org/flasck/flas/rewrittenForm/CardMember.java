package org.flasck.flas.rewrittenForm;

import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.commonBase.names.CardName;
import org.flasck.flas.commonBase.names.NameOfThing;
import org.flasck.flas.commonBase.names.VarName;
import org.flasck.flas.types.Type;
import org.zinutils.exceptions.UtilException;

public class CardMember implements ExternalRef {
	public final InputPosition location;
	public final CardName card;
	public final String var;
	public final Type type;
	private final VarName name;

	public CardMember(InputPosition location, CardName card, String var, Type type) {
		this.location = location;
		this.name = new VarName(location, card, var);
		this.card = card;
		this.var = var;
		if (type == null)
			throw new UtilException("Type is not allowed to be null");
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
		return name.uniqueName();
	}
	
	@Override
	public NameOfThing myName() {
		return name;
	}

	public boolean fromHandler() {
		throw new UtilException("This is not available");
	}
	
	@Override
	public String toString() {
		return "CardMember[" + uniqueName() + "]";
	}

}
