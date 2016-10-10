package org.flasck.flas.rewrittenForm;

import java.io.Serializable;

import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.parsedForm.Locatable;

@SuppressWarnings("serial")
public class PackageDefn implements Locatable, Serializable {
	public final InputPosition location;
	public final String name;

	public PackageDefn(InputPosition location, PackageDefn from) {
		this.location = location;
		this.name = from.name;
	}
	
	@Override
	public InputPosition location() {
		return location;
	}
}
