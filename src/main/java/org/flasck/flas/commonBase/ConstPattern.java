package org.flasck.flas.commonBase;

import org.flasck.flas.blockForm.InputPosition;

public class ConstPattern implements Pattern {
	public static final int INTEGER = 1;
	public static final int BOOLEAN = 2;

	public final int type;
	public final String value;
	public final InputPosition location;

	public ConstPattern(InputPosition loc, int type, String value) {
		this.location = loc;
		this.type = type;
		this.value = value;
	}
	
	@Override
	public InputPosition location() {
		return location;
	}
	
	@Override
	public String toString() {
		return value;
	}
}
