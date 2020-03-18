package org.flasck.flas.parsedForm;

import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.commonBase.names.CSName;

public class Provides extends Implements {
	public Provides(InputPosition kw, InputPosition location, TypeReference type, CSName csn) {
		super(kw, location, type, csn);
	}
}
