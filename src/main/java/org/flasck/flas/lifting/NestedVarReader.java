package org.flasck.flas.lifting;

import java.util.Collection;

import org.flasck.flas.parsedForm.UnresolvedVar;
import org.flasck.flas.patterns.HSIOptions;

public interface NestedVarReader {
	Collection<HSIOptions> all();
	Collection<UnresolvedVar> vars();
}
