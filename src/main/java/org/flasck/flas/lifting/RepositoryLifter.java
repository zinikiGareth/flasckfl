package org.flasck.flas.lifting;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;
import java.util.TreeSet;

import org.flasck.flas.parsedForm.FunctionDefinition;
import org.flasck.flas.parsedForm.FunctionIntro;
import org.flasck.flas.parsedForm.UnresolvedVar;
import org.flasck.flas.repository.LeafAdapter;
import org.flasck.flas.repository.Repository;

public class RepositoryLifter extends LeafAdapter implements Lifter {
	private MappingStore ms;
	private MappingAnalyzer ma;
	private Set<FunctionDefinition> dull = new TreeSet<>();
	private Set<FunctionDefinition> interesting = new TreeSet<>();

	// This doesn't belong here but in some kind of "output" value (it's the dependency list) ... I've lost the plot ...
	private ArrayList<DependencyGroup> ordering;

	@Override
	public void lift(Repository r) {
		r.traverse(this);
		resolve();
	}

	@Override
	public void visitFunction(FunctionDefinition fn) {
		ms = new MappingStore();
		ma = new MappingAnalyzer(fn, ms);
	}

	@Override
	public void visitFunctionIntro(FunctionIntro fi) {
		ma.visitFunctionIntro(fi);
	}

	@Override
	public void visitUnresolvedVar(UnresolvedVar vr, int nargs) {
		if (ma != null)
			ma.visitUnresolvedVar(vr);
	}

	@Override
	public void leaveFunction(FunctionDefinition fn) {
		if (ms.isInteresting()) {
			fn.nestedVars(ms);
			interesting.add(fn);
		} else {
			dull.add(fn);
		}
		ms = null;
	}

	// Resolve all fn-to-fn references
	public void resolve() {
		// TODO: we should probably have more direct unit tests of this
		// It possibly should also have its own class of some kind
		Set<FunctionDefinition> resolved = new TreeSet<>(dull);
		ordering = new ArrayList<>();
		while (resolved.size() < interesting.size()) {
			boolean handled = false;
			for (FunctionDefinition fn : interesting) {
				NestedVarReader nv = fn.nestedVars();
				if (nv.containsReferencesNotIn(resolved)) {
					// TODO: all the cases around mutual recursion are buried here ... extract all the common ones and put them together
					// Can we do this by having a map and having the values be shared?  We can create new ones as needed, and then replace all of them with a "merged" one when we find they're the same?
					// Note this is more reliable than trying to guess because if f depends on g and e depends on g but when we get to g it depends on both, we will already have two structures
					continue;
				}
				resolved.add(fn);
				handled = true;
				for (FunctionDefinition r : nv.references()) {
					// TODO: failing because you can't just copy the pattern opts ... it needs the FIs rewriting
					// In fact, I'm increasingly dubious about the whole switching thing.  I think we should make them all just VarPatterns
					// The switch will already have been done.
					nv.enhanceWith(fn,r.nestedVars());
				}
			}
			if (!handled)
				throw new RuntimeException("Failed to make progress " + interesting + " " + resolved);
		}
	}
}
