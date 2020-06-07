package org.flasck.flas.commonBase;

import org.flasck.flas.parsedForm.LogicHolder;

public interface Pattern extends Locatable {
	public void isDefinedBy(LogicHolder definedBy);
	public LogicHolder definedBy();
}
