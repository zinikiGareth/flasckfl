package org.flasck.flas.lifting;

import java.util.Collection;
import java.util.List;

import org.flasck.flas.parsedForm.UnresolvedVar;
import org.flasck.flas.patterns.HSIOptions;

public interface NestedVarReader {
	int size();
	Collection<HSIOptions> all();
	List<UnresolvedVar> vars();
}
