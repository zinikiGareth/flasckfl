package org.flasck.flas.lifting;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import org.flasck.flas.parsedForm.FunctionDefinition;
import org.flasck.flas.parsedForm.FunctionIntro;
import org.flasck.flas.parsedForm.StandaloneDefn;
import org.flasck.flas.parsedForm.StandaloneMethod;
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
	private Set<StandaloneDefn> dull = new TreeSet<>();
	private Set<StandaloneDefn> interesting = new TreeSet<>();

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
	public void visitStandaloneMethod(StandaloneMethod meth) {
		// does this need to do anything?
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

	
	@Override
	public void leaveStandaloneMethod(StandaloneMethod meth) {
		dull.add(meth);
	}

	// Resolve all fn-to-fn references
	// Return the ordering for the benefit of unit tests
	public FunctionGroupOrdering resolve() {
		// TODO: we should probably have more direct unit tests of this
		// It possibly should also have its own class of some kind
		ordering = new ArrayList<>();
		
		for (StandaloneDefn f : dull)
			ordering.add(new DependencyGroup(f));

		Set<StandaloneDefn> processedFns = new TreeSet<>(dull);
		Set<StandaloneDefn> remainingFns = new TreeSet<>(interesting);
		while (!remainingFns.isEmpty()) {
			boolean handled = false;
			Set<StandaloneDefn> done = new HashSet<>();
			for (StandaloneDefn fn : remainingFns) {
				NestedVarReader nv = fn.nestedVars();
				if (nv.containsReferencesNotIn(processedFns)) {
					continue;
				}
				processedFns.add(fn);
				done.add(fn);
				handled = true;
				for (StandaloneDefn r : nv.references()) {
					nv.enhanceWith(fn, r.nestedVars());
				}
				ordering.add(new DependencyGroup(fn));
			}
			remainingFns.removeAll(done);
			if (!handled) {
				// if we can't make progress, you have to assume that some mutual recursion is at play ... try everything in turn ... then pick the least complex one
				Set<Set<StandaloneDefn>> options = new TreeSet<>(new SizeComparator());
				for (StandaloneDefn fn : remainingFns) {
					Set<StandaloneDefn> tc = buildTransitiveClosure(fn, processedFns);
					if (tc != null)
						options.add(tc);
				}
				if (!options.isEmpty()) {
					Set<StandaloneDefn> tc = options.iterator().next();
					ordering.add(new DependencyGroup(tc));
					processedFns.addAll(tc);
					remainingFns.removeAll(tc);
				} else
					throw new RuntimeException("Failed to make progress: " + remainingFns + " -- " + processedFns);
			}
		}
		return new FunctionGroupOrdering(ordering);
	}

	private Set<StandaloneDefn> buildTransitiveClosure(StandaloneDefn fn, Set<StandaloneDefn> resolved) {
		Set<StandaloneDefn> closure = new TreeSet<>();
		buildMaximalTransitiveClosure(fn, resolved, closure);
		for (StandaloneDefn f : closure) {
			if (!checkFn(f, closure))
				return null;
		}
		return closure;
	}

	private boolean checkFn(StandaloneDefn f, Set<StandaloneDefn> closure) {
		for (StandaloneDefn g : closure) {
			NestedVarReader nv = g.nestedVars();
			if (nv.dependsOn(f))
				return true;
		}
		return false;
	}

	private void buildMaximalTransitiveClosure(StandaloneDefn fn, Set<StandaloneDefn> resolved, Set<StandaloneDefn> closure) {
		if (closure.contains(fn))
			return;
		closure.add(fn);
		NestedVarReader nv = fn.nestedVars();
		Set<StandaloneDefn> added = new TreeSet<>(nv.references());
		added.removeAll(resolved);
		added.removeAll(closure);
		for (StandaloneDefn o : added) {
			buildMaximalTransitiveClosure(o, resolved, closure);
		}
	}
}
