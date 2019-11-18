package org.flasck.flas.parsedForm;

import java.util.List;

import org.flasck.flas.commonBase.Locatable;
import org.flasck.flas.commonBase.names.FunctionName;
import org.flasck.flas.hsi.Slot;
import org.flasck.flas.patterns.HSITree;
import org.flasck.flas.repository.HSICases;

public interface LogicHolder extends Locatable {
	FunctionName name();
	HSITree hsiTree();
	List<Slot> slots();
	HSICases hsiCases();
}
