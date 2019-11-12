package org.flasck.flas.lifting;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import org.flasck.flas.parsedForm.FunctionDefinition;
import org.flasck.flas.parsedForm.FunctionIntro;
import org.flasck.flas.parsedForm.ObjectMethod;
import org.flasck.flas.parsedForm.StandaloneDefn;
import org.flasck.flas.parsedForm.StandaloneMethod;
import org.flasck.flas.parsedForm.UnresolvedVar;
import org.flasck.flas.repository.FunctionGroup;
import org.flasck.flas.repository.FunctionGroups;
import org.flasck.flas.repository.LeafAdapter;
import org.flasck.flas.repository.Repository;

public class RepositoryLifter extends LeafAdapter implements Lifter {
	public class LiftingGroup implements Comparable<LiftingGroup> {
		private final Set<StandaloneDefn> members = new TreeSet<>();
		private final String leader;

		public LiftingGroup(Set<StandaloneDefn> members) {
			this.members.addAll(members);
			this.leader = this.members.iterator().next().name().uniqueName();
		}
		
		@Override
		public int compareTo(LiftingGroup o) {
			int ret = Integer.compare(members.size(), o.members.size());
			if (ret != 0)
				return ret;
			return leader.compareTo(o.leader);
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
		dependencies.recordFunction(meth);
		ms = new MappingStore();
		ma = new MappingAnalyzer(meth, ms, dependencies);
	}

	@Override
	public void visitObjectMethod(ObjectMethod meth) {
		if (ma != null)
			ma.visitObjectMethod(meth);
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
		ma = null;
		ms = null;
	}

	
	@Override
	public void leaveStandaloneMethod(StandaloneMethod meth) {
		if (ms.isInteresting()) {
			meth.nestedVars(ms);
			interesting.add(meth);
		} else {
			dull.add(meth);
		}
		ma = null;
		ms = null;
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
				process(fn, processedFns);
				done.add(fn);
				ordering.add(new DependencyGroup(fn));
				handled = true;
			}
			remainingFns.removeAll(done);
			if (!handled) {
				// if we can't make progress, you have to assume that some mutual recursion is at play ... try everything in turn ... then pick the least complex one
				Set<LiftingGroup> options = new TreeSet<>();
				for (StandaloneDefn fn : remainingFns) {
					Set<StandaloneDefn> tc = buildTransitiveClosure(fn, processedFns);
					if (tc != null)
						options.add(new LiftingGroup(tc));
				}
				if (!options.isEmpty()) {
					LiftingGroup tc = options.iterator().next();
					for (StandaloneDefn fn : tc.members)
						process(fn, processedFns);
					remainingFns.removeAll(tc.members);
					ordering.add(new DependencyGroup(tc.members));
				} else
					throw new RuntimeException("Failed to make progress: " + remainingFns + " -- " + processedFns);
			}
		}
		return new FunctionGroupOrdering(ordering);
	}

	private void process(StandaloneDefn fn, Set<StandaloneDefn> processedFns) {
		processedFns.add(fn);
		NestedVarReader nv = fn.nestedVars();
		for (StandaloneDefn r : nv.references()) {
			nv.enhanceWith(fn, r.nestedVars());
		}
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
