package org.flasck.flas.hsi;

import org.flasck.flas.patterns.HSIOptions;
import org.zinutils.exceptions.NotImplementedException;

public class HLSlot implements Slot {
	private final String id;

	public HLSlot(String id) {
		this.id = id;
	}

	@Override
	public HSIOptions getOptions() {
		throw new NotImplementedException();
	}

	@Override
	public int score() {
		throw new NotImplementedException();
	}

	@Override
	public String id() {
		return id;
	}
	
	@Override
	public String toString() {
		return "HLSlot[" + id + "]";
	}
}
