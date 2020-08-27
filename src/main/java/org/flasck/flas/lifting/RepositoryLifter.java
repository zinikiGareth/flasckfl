package org.flasck.flas.lifting;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import org.flasck.flas.commonBase.MemberExpr;
import org.flasck.flas.commonBase.Pattern;
import org.flasck.flas.commonBase.names.FunctionName;
import org.flasck.flas.commonBase.names.HandlerName;
import org.flasck.flas.parsedForm.FunctionDefinition;
import org.flasck.flas.parsedForm.FunctionIntro;
import org.flasck.flas.parsedForm.HandlerImplements;
import org.flasck.flas.parsedForm.HandlerLambda;
import org.flasck.flas.parsedForm.LogicHolder;
import org.flasck.flas.parsedForm.ObjectCtor;
import org.flasck.flas.parsedForm.ObjectMethod;
import org.flasck.flas.parsedForm.StandaloneMethod;
import org.flasck.flas.parsedForm.TupleAssignment;
import org.flasck.flas.parsedForm.TupleMember;
import org.flasck.flas.parsedForm.TypedPattern;
import org.flasck.flas.parsedForm.UnresolvedVar;
import org.flasck.flas.parsedForm.VarPattern;
import org.flasck.flas.repository.FunctionGroup;
import org.flasck.flas.repository.FunctionGroups;
import org.flasck.flas.repository.LeafAdapter;
import org.flasck.flas.repository.LoadBuiltins;
import org.flasck.flas.repository.Repository;
import org.flasck.flas.repository.Traverser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.zinutils.collections.ListMap;
import org.zinutils.collections.SetMap;
import org.zinutils.exceptions.CantHappenException;

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
		
		@Override
		public String toString() {
			return "Group" + members;
		}
	}
	private LiftingDependencyMapper dependencies = new LiftingDependencyMapper();
	private MappingStore ms;
	private MappingAnalyzer ma;
	private Set<LogicHolder> dull = new TreeSet<>();
	private Set<LogicHolder> interesting = new TreeSet<>();

	private List<FunctionGroup> ordering;
	private ListMap<HandlerName, LogicHolder> his = new ListMap<>();

	@Override
	public FunctionGroups lift(Repository r) {
		r.traverse(this);
		logger.info("dull:");
		for (LogicHolder lh : dull) {
			logger.info("  " + lh.name());
		}
		logger.info("interesting:");
		for (LogicHolder lh : interesting) {
			logger.info("  " + lh.name() + " => " + lh.nestedVars().vars());
			logger.info("    depends on " + lh.nestedVars().references() + " " + lh.nestedVars().referencesHI());
		}
		enhanceAll();
		refhandlers();
		resolve();
		logger.info("group ordering = " + ordering);
		int k = 0;
		for (FunctionGroup grp : ordering) {
			logger.info("Group #" + (k++) + ": " + grp.size());
			for (LogicHolder lh : grp.functions())
				logger.info("  " + lh.name() + (lh.nestedVars() != null ? " => " + lh.nestedVars().vars() : ""));
		}
		return new FunctionGroupOrdering(ordering);
	}

	@Override
	public void visitHandlerImplements(HandlerImplements hi) {
		logger.info("saw HI " + hi);
		his.ensure(hi.handlerName);
	}
	
	@Override
	public void visitFunction(FunctionDefinition fn) {
		dependencies.recordFunction(fn);
		ms = new MappingStore(fn.name());
		ma = new MappingAnalyzer(fn, ms, dependencies);
	}

	@Override
	public void visitStandaloneMethod(StandaloneMethod meth) {
	}
	
	@Override
	public void visitTuple(TupleAssignment ta) {
		dependencies.recordFunction(ta);
		ms = new MappingStore(ta.name());
		ma = new MappingAnalyzer(ta, ms, dependencies);
	}
	
	@Override
	public void visitObjectMethod(ObjectMethod meth) {
		if (meth.name().container() instanceof HandlerName)
			his.add((HandlerName) meth.name().container(), meth);
		dependencies.recordFunction(meth);
		ms = new MappingStore(meth.name());
		ma = new MappingAnalyzer(meth, ms, dependencies);
		if (ma != null)
			ma.visitObjectMethod(meth);
	}
	
	@Override
	public void visitObjectCtor(ObjectCtor meth) {
		dependencies.recordFunction(meth);
		ms = new MappingStore(meth.name());
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
	public void visitMemberExpr(MemberExpr expr, int nargs) {
		if (ma != null)
			ma.visitDefn(expr.defn());
	}

	@Override
	public void leaveFunction(FunctionDefinition fn) {
		if (ms.isInteresting()) {
			fn.nestedVars(ms);
			interesting.add(fn);
		} else {
			dull.add(fn);
		}
		if (!fn.isObjAccessor())
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
		meth.reportHolderInArgCount();
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
			MappingStore msm = new MappingStore(ta.name());
			msm.recordDependency(ta);
			tm.nestedVars(msm);
			interesting.add(tm);
		}
		ma = null;
		ms = null;
	}

	private void enhanceAll() {
		boolean hasMore = true;
		while (hasMore) {
			hasMore = false;
			for (LogicHolder f : interesting) {
				hasMore |= enhance(f);
			}
		}
	}

	// If a method references handlers, that means it references all the methods in the handler ...
	private void refhandlers() {
		for (LogicHolder f : interesting) {
			for (HandlerImplements hi : f.nestedVars().referencesHI()) {
				final Map<String, Pattern> scoped = new TreeMap<>();
				final SetMap<String, FunctionName> users = new SetMap<>();
				logger.info(f.name() + " depends on " + hi.handlerName);
				for (LogicHolder m : his.get(hi.handlerName)) {
					logger.info("  " + f.name() + " therefore depends on " + m.name());
					f.nestedVars().dependsOn(m);
					if (m.nestedVars() == null)
						continue;
					m.nestedVars().dependsOn(f);
					for (Pattern p : m.nestedVars().patterns()) {
						if (p instanceof TypedPattern) {
							TypedPattern tp = (TypedPattern) p;
							scoped.put(tp.var.uniqueName(), tp);
							users.add(tp.var.uniqueName(), m.name());
						} else if (p instanceof VarPattern) {
							VarPattern vp = (VarPattern) p;
							scoped.put(vp.name().uniqueName(), vp);
							users.add(vp.name().uniqueName(), m.name());
						} else
							throw new CantHappenException("cannot handle pattern " + p + " as nested var");
					}
					m.nestedVars().clearPatterns();
				}
				int pos = 0;
				for (Entry<String, Pattern> q : scoped.entrySet()) {
					Pattern p = q.getValue();
					HandlerLambda hl = new HandlerLambda(p, true);
					for (FunctionName n : users.get(q.getKey()))
						hl.usedBy(n);
					hi.boundVars.add(pos++, hl);
					Traverser trav = new Traverser(new HLRewriter(p, hl)).withImplementedMethods();
					trav.visitHandlerImplements(hi);
				}
			}
		}
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
			logger.debug("resolving " + remainingFns);
			boolean handled = false;
			Set<LogicHolder> done = new HashSet<>();
			for (LogicHolder fn : remainingFns) {
				NestedVarReader nv = fn.nestedVars();
				if (nv.containsReferencesNotIn(processedFns)) {
					logger.debug("cannot handle " + fn + " because it has " + nv.references());
					continue;
				}
				logger.debug("extracted " + fn + " as a candidate group");
				process(fn, processedFns);
				done.add(fn);
				ordering.add(new DependencyGroup(fn));
				handled = true;
			}
			logger.debug("removing " + done + " from " + remainingFns);
			remainingFns.removeAll(done);
			if (!handled) {
				// if we can't make progress, you have to assume that some mutual recursion is at play ... try everything in turn ... then pick the least complex one
				Set<LiftingGroup> options = new TreeSet<>();
				for (LogicHolder fn : remainingFns) {
					Set<LogicHolder> tc = buildTransitiveClosure(fn, processedFns);
					if (tc != null)
						options.add(new LiftingGroup(tc));
				}
				logger.debug("failed to make progress in resolution, so trying to find an option from " + options);
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

	private boolean enhance(LogicHolder f) {
		boolean more = false;
		NestedVarReader nv = f.nestedVars();
		for (LogicHolder r : nv.references()) {
			more |= nv.enhanceWith(f, r.nestedVars());
		}
		return more;
	}

	private void process(LogicHolder fn, Set<LogicHolder> processedFns) {
		processedFns.add(fn);
	}

	private Set<LogicHolder> buildTransitiveClosure(LogicHolder fn, Set<LogicHolder> resolved) {
		Set<LogicHolder> closure = new TreeSet<>();
		buildMaximalTransitiveClosure(fn, resolved, closure);
		logger.info("built maximal transitive closure " + closure);
		for (LogicHolder f : closure) {
			if (!checkFn(f, closure))
				return null;
		}
		return closure;
	}

	private boolean checkFn(LogicHolder f, Set<LogicHolder> closure) {
		logger.debug("checking if " + f + " is integral to " + closure);
		for (LogicHolder g : closure) {
			NestedVarReader nv = g.nestedVars();
			logger.debug("  for " + g + " nv = " + nv + (nv != null ? " " + nv.references() + " and " + nv.dependsOn(f) : ""));
			if (nv != null && nv.dependsOn(f))
				return true;
		}
		logger.info("rejecting " + closure + " because none of them depend on " + f);
		return false;
	}

	private void buildMaximalTransitiveClosure(LogicHolder fn, Set<LogicHolder> resolved, Set<LogicHolder> closure) {
		if (closure.contains(fn))
			return;
		closure.add(fn);
		NestedVarReader nv = fn.nestedVars();
		if (nv != null) {
			Set<LogicHolder> added = new TreeSet<>(nv.references());
			added.addAll(nv.referencesHIMethods());
			added.removeAll(resolved);
			added.removeAll(closure);
			for (LogicHolder o : added) {
				buildMaximalTransitiveClosure(o, resolved, closure);
			}
		}
	}
}
