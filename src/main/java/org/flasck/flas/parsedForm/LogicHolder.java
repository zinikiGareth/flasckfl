package org.flasck.flas.parsedForm;

import java.util.List;

import org.flasck.flas.commonBase.Locatable;
import org.flasck.flas.commonBase.names.FunctionName;
import org.flasck.flas.commonBase.names.NameOfThing;
import org.flasck.flas.hsi.Slot;
import org.flasck.flas.lifting.NestedVarReader;
import org.flasck.flas.patterns.HSITree;
import org.flasck.flas.repository.HSICases;

public interface LogicHolder extends Locatable, Comparable<LogicHolder> {
	FunctionName name();
	boolean isMyName(NameOfThing other);
	boolean hasArgs();
	boolean hasState();
	StateHolder state();
	void setConstness(FunctionConstness fc);
	FunctionConstness constNess();

	List<Slot> slots();
	HSITree hsiTree();
	HSICases hsiCases();

	void nestedVars(NestedVarReader nestedVars);
	NestedVarReader nestedVars();
	
	boolean isObjAccessor();
	boolean generate();
}
