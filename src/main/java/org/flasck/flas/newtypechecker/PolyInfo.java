package org.flasck.flas.newtypechecker;

import org.flasck.flas.blockForm.InputPosition;
import org.zinutils.exceptions.UtilException;

public class PolyInfo extends TypeInfo {
	private final InputPosition location;
	public final String name;

	public PolyInfo(InputPosition location, String name) {
		if (location == null)
			throw new UtilException("PolyInfo without input location");
		this.location = location;
		this.name = name;
	}
	
	public InputPosition location() {
		return location;
	}

	@Override
	public String toString() {
		return "PV[" + name + "]";
	}
}
