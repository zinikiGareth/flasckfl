package org.flasck.flas.flim;

import java.util.Set;
import java.util.TreeSet;

import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.commonBase.Locatable;
import org.flasck.flas.rewrittenForm.RWStructDefn;

public class ImportedCard extends ImportedContract implements Locatable {
	private final InputPosition location;
	public final RWStructDefn struct;
	public final Set<ImportedContract> contracts = new TreeSet<ImportedContract>();
	public final Set<ImportedContract> handlers = new TreeSet<ImportedContract>();

	// Used when parsing and generating
	public ImportedCard(RWStructDefn struct) {
		super(struct.name());
		this.struct = struct;
		this.location = struct.location();
	}

	// External view loaded back in from FLIM
	public ImportedCard(InputPosition location, String name) {
		super(name);
		this.location = location;
		this.struct = null;
	}

	@Override
	public InputPosition location() {
		return location;
	}
}
