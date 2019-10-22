package org.flasck.flas.lifting;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import org.flasck.flas.commonBase.names.FunctionName;
import org.flasck.flas.parsedForm.FunctionDefinition;
import org.flasck.flas.parsedForm.FunctionIntro;
import org.flasck.flas.parsedForm.UnresolvedVar;
import org.flasck.flas.repository.FunctionGroup;
import org.flasck.flas.repository.FunctionGroups;
import org.flasck.flas.repository.LeafAdapter;
import org.flasck.flas.repository.Repository;

public class RepositoryLifter extends LeafAdapter implements Lifter {
	public class SizeComparator implements Comparator<Set<?>> {
		@Override
		public int compare(Set<?> o1, Set<?> o2) {
			return Integer.compare(o1.size(), o2.size());
		}
	}

	private LiftingDependencyMapper dependencies = new LiftingDependencyMapper();
	private MappingStore ms;
	private MappingAnalyzer ma;
	private Set<FunctionDefinition> dull = new TreeSet<>();
	private Set<FunctionDefinition> interesting = new TreeSet<>();

	private List<FunctionGroup> ordering;

	@Override
	public FunctionGroups lift(Repository r) {
		r.traverse(this);
		resolve();
		return new FunctionGroupOrdering(ordering);
	}

	@Override
	public void visitFunction(FunctionDefinition fn) {
		dependencies.recordFunction(fn);
		ms = new MappingStore();
		ma = new MappingAnalyzer(fn, ms, dependencies);
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
	public FunctionGroupOrdering resolve() {
		// TODO: we should probably have more direct unit tests of this
		// It possibly should also have its own class of some kind
		ordering = new ArrayList<>();
		Set<FunctionDefinition> resolved = new TreeSet<>(dull);
		for (FunctionDefinition f : dull)
			ordering.add(new DependencyGroup(f));
		Set<FunctionDefinition> remaining = new TreeSet<>(interesting);
		while (!remaining.isEmpty()) {
			boolean handled = false;
			Set<FunctionDefinition> done = new HashSet<>();
			for (FunctionDefinition fn : remaining) {
				NestedVarReader nv = fn.nestedVars();
				if (nv.containsReferencesNotIn(resolved)) {
					continue;
				}
				resolved.add(fn);
				done.add(fn);
				handled = true;
				for (FunctionDefinition r : nv.references()) {
					nv.enhanceWith(fn, r.nestedVars());
				}
				ordering.add(new DependencyGroup(fn));
			}
			remaining.removeAll(done);
			if (!handled) {
				// if we can't make progress, you have to assume that some mutual recursion is at play ... try everything in turn ... then pick the least complex one
				Set<Set<FunctionDefinition>> options = new TreeSet<>(new SizeComparator());
				for (FunctionDefinition fn : remaining) {
					Set<FunctionDefinition> tc = buildTransitiveClosure(fn, resolved);
					if (tc != null)
						options.add(tc);
				}
				if (!options.isEmpty()) {
					Set<FunctionDefinition> tc = options.iterator().next();
					ordering.add(new DependencyGroup(tc));
					resolved.addAll(tc);
					remaining.removeAll(tc);
				} else
					throw new RuntimeException("Failed to make progress: " + remaining + " -- " + resolved);
			}
		}
		return new FunctionGroupOrdering(ordering);
	}

	private Set<FunctionDefinition> buildTransitiveClosure(FunctionDefinition fn, Set<FunctionDefinition> resolved) {
		Set<FunctionDefinition> closure = new TreeSet<>();
		buildMaximalTransitiveClosure(fn, resolved, closure);
		for (FunctionDefinition f : closure) {
			if (!checkFn(f, closure))
				return null;
		}
		return closure;
	}

	private boolean checkFn(FunctionDefinition f, Set<FunctionDefinition> closure) {
		for (FunctionDefinition g : closure) {
			NestedVarReader nv = g.nestedVars();
			if (nv.dependsOn(f))
				return true;
		}
		return false;
	}

	private void buildMaximalTransitiveClosure(FunctionDefinition fn, Set<FunctionDefinition> resolved, Set<FunctionDefinition> closure) {
		if (closure.contains(fn))
			return;
		closure.add(fn);
		NestedVarReader nv = fn.nestedVars();
		Set<FunctionDefinition> added = new TreeSet<FunctionDefinition>(nv.references());
		added.removeAll(resolved);
		added.removeAll(closure);
		for (FunctionDefinition o : added) {
			buildMaximalTransitiveClosure(o, resolved, closure);
		}
	}
}
