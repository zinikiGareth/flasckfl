package org.flasck.flas.repository;

import org.flasck.flas.parsedForm.FunctionDefinition;
import org.flasck.flas.parsedForm.StandaloneMethod;

public interface FunctionGroup {
	Iterable<FunctionDefinition> functions();
	Iterable<StandaloneMethod> standalones();
}
