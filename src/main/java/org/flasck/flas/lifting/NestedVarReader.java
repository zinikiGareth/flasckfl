package org.flasck.flas.lifting;

import java.util.Collection;
import java.util.List;
import java.util.Set;

import org.flasck.flas.parsedForm.StandaloneDefn;
import org.flasck.flas.parsedForm.UnresolvedVar;
import org.flasck.flas.patterns.HSIOptions;

public interface NestedVarReader {
	int size();
	Collection<HSIOptions> all();
	List<UnresolvedVar> vars();
	List<Object> patterns();
	boolean containsReferencesNotIn(Set<StandaloneDefn> processedFns);
	Set<StandaloneDefn> references();
	void enhanceWith(StandaloneDefn fn, NestedVarReader nestedVars);
	boolean dependsOn(StandaloneDefn f);
}
