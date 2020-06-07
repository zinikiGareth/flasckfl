package org.flasck.flas.lifting;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import org.flasck.flas.parsedForm.FunctionDefinition;
import org.flasck.flas.parsedForm.FunctionIntro;
import org.flasck.flas.parsedForm.ObjectCtor;
import org.flasck.flas.parsedForm.ObjectDefn;
import org.flasck.flas.parsedForm.ObjectMethod;
import org.flasck.flas.parsedForm.LogicHolder;
import org.flasck.flas.parsedForm.StandaloneMethod;
import org.flasck.flas.parsedForm.TupleAssignment;
import org.flasck.flas.parsedForm.TupleMember;
import org.flasck.flas.parsedForm.UnresolvedVar;
import org.flasck.flas.repository.FunctionGroup;
import org.flasck.flas.repository.FunctionGroups;
import org.flasck.flas.repository.LeafAdapter;
import org.flasck.flas.repository.LoadBuiltins;
import org.flasck.flas.repository.Repository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RepositoryLifter extends LeafAdapter implements Lifter {
	public static final Logger logger = LoggerFactory.getLogger("Lifter");
	public class LiftingGroup implements Comparable<LiftingGroup> {
		private final Set<LogicHolder> members = new TreeSet<>();
		private final String leader;

		public LiftingGroup(Set<LogicHolder> members) {
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
	private Set<LogicHolder> dull = new TreeSet<>();
	private Set<LogicHolder> interesting = new TreeSet<>();

	private List<FunctionGroup> ordering;

	@Override
	public FunctionGroups lift(Repository r) {
		r.traverse(this);
		resolve();
		logger.info("group ordering = " + ordering);
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
//		dependencies.recordFunction(meth);
//		ms = new MappingStore();
//		ma = new MappingAnalyzer(meth, ms, dependencies);
	}
	
	@Override
	public void visitTuple(TupleAssignment ta) {
		dependencies.recordFunction(ta);
		ms = new MappingStore();
		ma = new MappingAnalyzer(ta, ms, dependencies);
	}
	
	@Override
	public void visitObjectMethod(ObjectMethod meth) {
		dependencies.recordFunction(meth);
		ms = new MappingStore();
		ma = new MappingAnalyzer(meth, ms, dependencies);
		if (ma != null)
			ma.visitObjectMethod(meth);
	}
	
	@Override
	public void visitObjectCtor(ObjectCtor meth) {
		dependencies.recordFunction(meth);
		ms = new MappingStore();
		ma = new MappingAnalyzer(meth, ms, dependencies);
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
		if (!(fn.state() instanceof ObjectDefn))
			fn.reportHolderInArgCount();
		ma = null;
		ms = null;
	}

	@Override
	public void leaveObjectMethod(ObjectMethod meth) {
		if (ms.isInteresting()) {
			meth.nestedVars(ms);
			interesting.add(meth);
		} else {
			dull.add(meth);
		}
		ma = null;
		ms = null;
	}
	
	@Override
	public void leaveObjectCtor(ObjectCtor meth) {
		if (ms.isInteresting()) {
			meth.nestedVars(ms);
			interesting.add(meth);
		} else {
			dull.add(meth);
		}
		ma = null;
		ms = null;
	}
	
	@Override
	public void leaveStandaloneMethod(StandaloneMethod meth) {
//		if (ms.isInteresting()) {
//			meth.nestedVars(ms);
//			interesting.add(meth);
//		} else {
//			dull.add(meth);
//		}
		meth.reportHolderInArgCount();
//		ma = null;
//		ms = null;
	}

	@Override
	public void leaveTuple(TupleAssignment ta) {
		if (ms.isInteresting()) {
			ta.nestedVars(ms);
			interesting.add(ta);
		} else {
			dull.add(ta);
		}
		for (TupleMember tm : ta.members) {
			MappingStore msm = new MappingStore();
			msm.recordDependency(ta);
			tm.nestedVars(msm);
			interesting.add(tm);
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
		
		for (LogicHolder f : dull)
			ordering.add(new DependencyGroup(f));

		Set<LogicHolder> processedFns = new TreeSet<>(LoadBuiltins.allFunctions);
		processedFns.addAll(dull);
		Set<LogicHolder> remainingFns = new TreeSet<>(interesting);
		while (!remainingFns.isEmpty()) {
			boolean handled = false;
			Set<LogicHolder> done = new HashSet<>();
			for (LogicHolder fn : remainingFns) {
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
				for (LogicHolder fn : remainingFns) {
					Set<LogicHolder> tc = buildTransitiveClosure(fn, processedFns);
					if (tc != null)
						options.add(new LiftingGroup(tc));
				}
				if (!options.isEmpty()) {
					LiftingGroup tc = options.iterator().next();
					for (LogicHolder fn : tc.members)
						process(fn, processedFns);
					remainingFns.removeAll(tc.members);
					ordering.add(new DependencyGroup(tc.members));
				} else
					throw new RuntimeException("Failed to make progress: " + remainingFns + " -- " + processedFns);
			}
		}
		return new FunctionGroupOrdering(ordering);
	}

	private void process(LogicHolder fn, Set<LogicHolder> processedFns) {
		processedFns.add(fn);
		NestedVarReader nv = fn.nestedVars();
		for (LogicHolder r : nv.references()) {
			nv.enhanceWith(fn, r.nestedVars());
		}
	}

	private Set<LogicHolder> buildTransitiveClosure(LogicHolder fn, Set<LogicHolder> resolved) {
		Set<LogicHolder> closure = new TreeSet<>();
		buildMaximalTransitiveClosure(fn, resolved, closure);
		for (LogicHolder f : closure) {
			if (!checkFn(f, closure))
				return null;
		}
		return closure;
	}

	private boolean checkFn(LogicHolder f, Set<LogicHolder> closure) {
		for (LogicHolder g : closure) {
			NestedVarReader nv = g.nestedVars();
			if (nv != null && nv.dependsOn(f))
				return true;
		}
		return false;
	}

	private void buildMaximalTransitiveClosure(LogicHolder fn, Set<LogicHolder> resolved, Set<LogicHolder> closure) {
		if (closure.contains(fn))
			return;
		closure.add(fn);
		NestedVarReader nv = fn.nestedVars();
		if (nv != null) {
			Set<LogicHolder> added = new TreeSet<>(nv.references());
			added.removeAll(resolved);
			added.removeAll(closure);
			for (LogicHolder o : added) {
				buildMaximalTransitiveClosure(o, resolved, closure);
			}
		}
	}
}
