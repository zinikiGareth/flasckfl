package org.flasck.flas.typechecker;

import java.util.Set;
import java.util.TreeSet;

import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.commonBase.Locatable;
import org.flasck.flas.rewrittenForm.CardGrouping;
import org.flasck.flas.rewrittenForm.RWStructDefn;

public class CardTypeInfo extends TypeHolder implements Locatable {
	public final RWStructDefn struct;
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
