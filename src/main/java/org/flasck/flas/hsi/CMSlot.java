package org.flasck.flas.hsi;

import org.flasck.flas.patterns.HSIOptions;
import org.zinutils.exceptions.NotImplementedException;

public class CMSlot implements Slot {
	private final HSIOptions contained;

	public CMSlot(HSIOptions contained) {
		this.contained = contained;
	}

	@Override
	public HSIOptions getOptions() {
		return contained;
	}

	@Override
	public int score() {
		throw new NotImplementedException();
	}

}
