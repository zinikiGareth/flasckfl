package org.flasck.flas.parsedForm;

import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.commonBase.names.CSName;
import org.flasck.flas.tc3.NamedType;

public class Provides extends Implements {
	public Provides(InputPosition kw, InputPosition location, NamedType consumer, TypeReference type, CSName csn) {
		super(kw, location, consumer, type, csn);
	}
}
