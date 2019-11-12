package org.flasck.flas.commonBase;

import org.flasck.flas.parsedForm.StandaloneDefn;

public interface Pattern extends Locatable {
	public void isDefinedBy(StandaloneDefn definedBy);
	public StandaloneDefn definedBy();
}
