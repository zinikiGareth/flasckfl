package org.flasck.flas.repository;

import org.flasck.flas.parsedForm.LogicHolder;

public interface FunctionGroup {
	boolean isEmpty();
	Iterable<LogicHolder> functions();
	int size();
}
