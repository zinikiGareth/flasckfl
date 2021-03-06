package org.flasck.flas.errors;

import java.util.Set;
import java.util.TreeSet;

import org.flasck.flas.blockForm.InputPosition;

public class FLASError implements Comparable<FLASError> {
	public final InputPosition loc;
	public final String msg;
	public final Set<InputPosition> others = new TreeSet<>(); 

	public FLASError(InputPosition loc, String msg) {
		this.loc = loc;
		this.msg = msg;
	}
	
	public void otherLoc(InputPosition pos) {
		others.add(pos);
	}

	@Override
	public String toString() {
		return "" + loc + ": " + msg;
	}

	@Override
	public int compareTo(FLASError o) {
		if (loc != null && o.loc == null)
			return 1;
		else if (loc == null && o.loc != null)
			return -1;
		else if (loc != null && o.loc != null) {
			int dir = loc.compareTo(o.loc);
			if (dir != 0)
				return dir;
		}
		return msg.compareTo(o.msg);
	}
}
