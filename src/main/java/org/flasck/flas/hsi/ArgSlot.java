package org.flasck.flas.hsi;

import org.flasck.flas.patterns.HSIOptions;

public class ArgSlot implements Slot {
	private final HSIOptions hsiOptions;

	public ArgSlot(int argPos, HSIOptions hsiOptions) {
		this.hsiOptions = hsiOptions;
	}

	@Override
	public HSIOptions getOptions() {
		return hsiOptions;
	}

	@Override
	public int score() {
		return hsiOptions.score();
	}

}
