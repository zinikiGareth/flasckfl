package org.flasck.flas.rewrittenForm;

import java.util.List;

import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.commonBase.names.NameOfThing;
import org.flasck.flas.commonBase.names.VarName;
import org.flasck.flas.types.Type;
import org.flasck.flas.vcode.hsieForm.PushExternal;
import org.flasck.flas.vcode.hsieForm.PushReturn;
import org.flasck.flas.vcode.hsieForm.VarInSource;
import org.zinutils.exceptions.UtilException;

public class CardMember implements ExternalRef {
	public final InputPosition location;
	public final NameOfThing card;
	public final String var;
	public final Type type;
	private final VarName name;

	public CardMember(InputPosition location, NameOfThing card, String var, Type type) {
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
	public PushReturn hsie(InputPosition loc, List<VarInSource> deps) {
		// TODO: replace this with something more specific
		return new PushExternal(location, this);
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
