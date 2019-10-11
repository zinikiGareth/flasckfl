package org.flasck.flas.lifting;

import java.util.List;

import org.flasck.flas.repository.FunctionGroup;
import org.flasck.flas.repository.Repository;

public interface Lifter {
	List<FunctionGroup> lift(Repository r);
}
