package org.flasck.flas.parsedForm;

import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.commonBase.names.CSName;
import org.flasck.flas.tc3.NamedType;

public class ImplementsContract extends Implements {
	public ImplementsContract(InputPosition kw, InputPosition location, NamedType parent, TypeReference type, CSName csn) {
		super(kw, location, parent, type, csn);
	}
	
	@Override
	public String toString() {
		return super.toString();
	}
}
