package org.flasck.flas.typechecker;

import java.util.Set;
import java.util.TreeSet;

import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.commonBase.Locatable;
import org.flasck.flas.rewrittenForm.RWStructDefn;

public class CardTypeInfo extends TypeHolder implements Locatable {
	private final InputPosition location;
	public final RWStructDefn struct;
	public final Set<TypeHolder> contracts = new TreeSet<TypeHolder>();
	public final Set<TypeHolder> handlers = new TreeSet<TypeHolder>();

	// Used when parsing and generating
	public CardTypeInfo(RWStructDefn struct) {
		super(struct.name());
		this.struct = struct;
		this.location = struct.location();
	}

	// External view loaded back in from FLIM
	public CardTypeInfo(InputPosition location, String name) {
		super(name);
		this.location = location;
		this.struct = null;
	}

	@Override
	public InputPosition location() {
		return location;
	}
}
