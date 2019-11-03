package org.flasck.flas.parsedForm;

import java.util.List;

import org.flasck.flas.commonBase.names.FunctionName;
import org.flasck.flas.hsi.Slot;
import org.flasck.flas.lifting.NestedVarReader;
import org.flasck.flas.patterns.HSITree;

public interface LogicHolder {
	FunctionName name();
//	NestedVarReader nestedVars();
	HSITree hsiTree();
//	List<Slot> slots();
}
