package org.flasck.flas.typechecker;

import java.io.Serializable;
import java.util.Set;
import java.util.TreeSet;

import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.parsedForm.CardGrouping;
import org.flasck.flas.parsedForm.Locatable;
import org.flasck.flas.parsedForm.StructDefn;

@SuppressWarnings("serial")
public class CardTypeInfo extends TypeHolder implements Locatable, Serializable {
	public final StructDefn struct;
	public final Set<TypeHolder> contracts = new TreeSet<TypeHolder>();
	public final Set<TypeHolder> handlers = new TreeSet<TypeHolder>();

	public CardTypeInfo(CardGrouping cg) {
		super(cg.struct.name());
		this.struct = cg.struct;
	}

	@Override
	public InputPosition location() {
		return struct.location();
	}
}
