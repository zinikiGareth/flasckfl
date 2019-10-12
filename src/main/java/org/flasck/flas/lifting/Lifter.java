package org.flasck.flas.lifting;

import org.flasck.flas.repository.FunctionGroups;
import org.flasck.flas.repository.Repository;

public interface Lifter {
	FunctionGroups lift(Repository r);
}
