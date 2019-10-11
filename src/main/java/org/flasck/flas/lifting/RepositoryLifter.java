package org.flasck.flas.lifting;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import org.flasck.flas.parsedForm.FunctionDefinition;
import org.flasck.flas.parsedForm.FunctionIntro;
import org.flasck.flas.parsedForm.UnresolvedVar;
import org.flasck.flas.repository.FunctionGroup;
import org.flasck.flas.repository.LeafAdapter;
import org.flasck.flas.repository.Repository;

public class RepositoryLifter extends LeafAdapter implements Lifter {
	private MappingStore ms;
	private MappingAnalyzer ma;
	private Set<FunctionDefinition> dull = new TreeSet<>();
	private Set<FunctionDefinition> interesting = new TreeSet<>();

	private List<FunctionGroup> ordering;

	@Override
	public List<FunctionGroup> lift(Repository r) {
		r.traverse(this);
		resolve();
		return ordering;
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
	// Return the ordering for the benefit of unit tests
	public List<FunctionGroup> resolve() {
		// TODO: we should probably have more direct unit tests of this
		// It possibly should also have its own class of some kind
		ordering = new ArrayList<>();
		Set<FunctionDefinition> resolved = new TreeSet<>(dull);
		for (FunctionDefinition f : dull)
			ordering.add(new DependencyGroup(f));
		Set<FunctionDefinition> remaining = new TreeSet<>(interesting);
		while (!remaining.isEmpty()) {
			boolean handled = false;
			for (FunctionDefinition fn : remaining) {
				NestedVarReader nv = fn.nestedVars();
				if (nv.containsReferencesNotIn(resolved)) {
					continue;
				}
				resolved.add(fn);
				remaining.remove(fn);
				handled = true;
				for (FunctionDefinition r : nv.references()) {
					// TODO: failing because you can't just copy the pattern opts ... it needs the FIs rewriting
					// In fact, I'm increasingly dubious about the whole switching thing.  I think we should make them all just VarPatterns
					// The switch will already have been done.
					nv.enhanceWith(fn, r.nestedVars());
				}
				ordering.add(new DependencyGroup(fn));
			}
			if (!handled) {
				// if we can't make progress, you have to assume that some mutual recursion is at play ... try everything in turn ..
				for (FunctionDefinition fn : remaining) {
					Set<FunctionDefinition> tc = buildTransitiveClosure(fn, resolved);
					if (tc != null) {
						ordering.add(new DependencyGroup(tc));
						resolved.addAll(tc);
						remaining.removeAll(tc);
						handled = true;
						break;
					}
				}			
				if (!handled)
					throw new RuntimeException("Failed to make progress: " + remaining + " -- " + resolved);
			}
		}
		return ordering;
	}

	private Set<FunctionDefinition> buildTransitiveClosure(FunctionDefinition fn, Set<FunctionDefinition> resolved) {
		NestedVarReader nv = fn.nestedVars();
		TreeSet<FunctionDefinition> others = new TreeSet<>(nv.references());
		others.removeAll(resolved);
		// in order to qualify, all these functions must reference fn
		for (FunctionDefinition o : others) {
			if (!o.nestedVars().dependsOn(fn))
				return null;
		}
		others.add(fn);
		return others;
	}
}
