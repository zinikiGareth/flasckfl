package org.flasck.flas.parsedForm;

import java.util.List;

import org.flasck.flas.commonBase.names.NameOfThing;
import org.flasck.flas.hsi.Slot;
import org.flasck.flas.lifting.NestedVarReader;

public interface StandaloneDefn extends LogicHolder {
	void nestedVars(NestedVarReader nestedVars);
	NestedVarReader nestedVars();
	List<Slot> slots();
	boolean isMyName(NameOfThing other);
}
