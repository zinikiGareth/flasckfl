package org.flasck.flas.hsi;

import org.flasck.flas.patterns.HSIOptions;
import org.flasck.flas.tc3.NamedType;

public class ArgSlot implements Slot {
	private final int argPos;
	private final HSIOptions hsiOptions;

	public ArgSlot(int argPos, HSIOptions hsiOptions) {
		this.argPos = argPos;
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

	public int argpos() {
		return argPos;
	}

	@Override
	public String id() {
		return Integer.toString(argPos);
	}

	public boolean isContainer() {
		return hsiOptions.isContainer();
	}

	public NamedType containerType() {
		return hsiOptions.containerType();
	}
	
	@Override
	public String toString() {
		return "ArgSlot[" + argPos + "]";
	}
}
