package org.flasck.flas.repository;

import org.flasck.flas.parsedForm.StandaloneDefn;

public interface FunctionGroup {
	Iterable<StandaloneDefn> functions();
}
