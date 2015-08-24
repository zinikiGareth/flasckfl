package org.flasck.flas.errors;

import org.flasck.flas.blockForm.InputPosition;

public class FLASError implements Comparable<FLASError> {
	public final InputPosition loc;
	public final String msg;

	public FLASError(InputPosition loc, String msg) {
		this.loc = loc;
		this.msg = msg;
	}
	
	@Override
	public String toString() {
		return "" + loc + ": " + msg;
	}

	@Override
	public int compareTo(FLASError o) {
		return loc.compareTo(o.loc);
	}
}
