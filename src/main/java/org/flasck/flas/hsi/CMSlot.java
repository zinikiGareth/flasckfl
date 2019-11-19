package org.flasck.flas.hsi;

import org.flasck.flas.patterns.HSIOptions;

public class CMSlot implements Slot {
	private final HSIOptions contained;
	private final String id;

	public CMSlot(String id, HSIOptions contained) {
		this.id = id;
		this.contained = contained;
	}

	@Override
	public HSIOptions getOptions() {
		return contained;
	}

	@Override
	public int score() {
		return contained.score();
	}

	@Override
	public String id() {
		return id;
	}
	
	@Override
	public String toString() {
		return "CMSlot[" + id + "]";
	}
}
