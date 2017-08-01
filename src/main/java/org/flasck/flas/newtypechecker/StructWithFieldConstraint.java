package org.flasck.flas.newtypechecker;

import org.flasck.flas.blockForm.InputPosition;

public class StructWithFieldConstraint extends TypeInfo {
	public final InputPosition posn;
	public final String fname;

	public StructWithFieldConstraint(InputPosition posn, String fname) {
		this.posn = posn;
		this.fname = fname;
	}

	@Override
	public String toString() {
		return "Must be struct containing a field " + fname;
	}
}
