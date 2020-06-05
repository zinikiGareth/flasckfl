package org.flasck.flas.tc3;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.commonBase.names.SolidName;
import org.flasck.flas.errors.ErrorReporter;
import org.flasck.flas.parsedForm.IntroduceVar;
import org.flasck.flas.parsedForm.PolyType;
import org.flasck.flas.parsedForm.StandaloneDefn;
import org.flasck.flas.parsedForm.TypeBinder;
import org.flasck.flas.parsedForm.VarPattern;
import org.flasck.flas.repository.FunctionGroup;
import org.flasck.flas.repository.LoadBuiltins;
import org.flasck.flas.repository.RepositoryReader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.zinutils.exceptions.HaventConsideredThisException;
import org.zinutils.exceptions.NotImplementedException;
import org.zinutils.graphs.DirectedAcyclicGraph;
import org.zinutils.graphs.Node;
import org.zinutils.graphs.NodeWalker;

public class FunctionGroupTCState implements CurrentTCState {
	private final static Logger logger = LoggerFactory.getLogger("TCUnification");
	private final RepositoryReader repository;
	private final Map<String, UnifiableType> constraints = new TreeMap<>();
	private final Map<VarPattern, UnifiableType> patts = new TreeMap<>(VarPattern.comparator);
	private final Map<IntroduceVar, UnifiableType> introductions = new TreeMap<>(IntroduceVar.comparator);
	int polyCount = 0;
	private List<UnifiableType> allUTs = new ArrayList<>();
	private final boolean hasGroup;
	
	public FunctionGroupTCState(RepositoryReader repository, FunctionGroup grp) {
		this.repository = repository;
		for (StandaloneDefn x : grp.functions())
			bindVarToUT(x.name().uniqueName(), createUT(x.location(), x.name().uniqueName() + " returns", false));
		this.hasGroup = !grp.isEmpty();
	}

	@Override
	public boolean hasGroup() {
		return hasGroup;
	}
	
	@Override
	public UnifiableType createUT(InputPosition pos, String motive) {
		return createUT(pos, motive, true);
	}

	@Override
	public UnifiableType createUT(InputPosition pos, String motive, boolean unionNeedsAll) {
		TypeConstraintSet ret = new TypeConstraintSet(repository, this, pos, "ret_" + allUTs.size(), motive, unionNeedsAll);
		allUTs.add(ret);
		return ret;
	}

	@Override
	public void bindVarToUT(String name, UnifiableType ty) {
		if (!allUTs.contains(ty))
			throw new NotImplementedException("Where did this come from?");
		constraints.put(name, ty);
	}

	@Override
	public void bindVarPatternToUT(VarPattern vp, UnifiableType ty) {
		if (!allUTs.contains(ty))
			throw new NotImplementedException("Where did this come from?");
		patts.put(vp, ty);
	}

	@Override
	public void bindIntroducedVarToUT(IntroduceVar v, UnifiableType ut) {
		if (!allUTs.contains(ut))
			throw new NotImplementedException("Where did this come from?");
		introductions.put(v, ut);
	}

	@Override
	public void bindVarPatternTypes(ErrorReporter errors) {
		for (Entry<VarPattern, UnifiableType> e : patts.entrySet()) {
			UnifiableType ut = e.getValue();
			e.getKey().bindType(ut.resolvedTo());
		}
	}

	@Override
	public void bindIntroducedVarTypes(ErrorReporter errors) {
		for (Entry<IntroduceVar, UnifiableType> e : introductions.entrySet()) {
			e.getKey().bindType(e.getValue());
		}
	}
	
	@Override
	public UnifiableType requireVarConstraints(InputPosition pos, String var) {
		if (!constraints.containsKey(var))
			throw new RuntimeException("We don't have var constraints for " + var + " but it should have been bound during arg processing");
		return constraints.get(var);
	}

	@Override
	public UnifiableType hasVar(String var) {
		return constraints.get(var);
	}

	@Override
	public PolyType nextPoly(InputPosition pos) {
		if (polyCount >= 26)
			throw new NotImplementedException("Cannot handle more than 26 poly types at once");
		return new PolyType(pos, new SolidName(null, new String(new char[] { (char)('A' + polyCount++) })));
	}
	
	
	@Override
	public void groupDone(ErrorReporter errors, Map<TypeBinder, PosType> memberTypes) {
		// TODO: should we use an ErrorMark so as to stop when errors occur and avoid cascades?
		TypeChecker.logger.debug("starting to check group: " + memberTypes.keySet());
		for (Entry<TypeBinder, PosType> e : memberTypes.entrySet())
			logger.debug(e.getKey() + " :: " + e.getValue().type);
		
		// if we picked up anything based on the invocation of the method in this group, add that into the mix
		for (Entry<TypeBinder, PosType> m : memberTypes.entrySet()) {
			String name = m.getKey().name().uniqueName();
			UnifiableType ut = this.requireVarConstraints(m.getKey().location(), name);
			ut.determinedType(m.getValue());
		}
		this.debugInfo("initial");

		expandUsed();
		this.debugInfo("moreused");

		expandUnions();
		this.debugInfo("expanded");

		mergePolyVars();
		this.debugInfo("merged");

		acquireEquivalent();
		this.debugInfo("acquired");
		
		DirectedAcyclicGraph<UnifiableType> dag = collate(errors);
		logger.debug("UT DAG:\n" + dag.toString());
		List<UnifiableType> roots = dag.roots();
		Comparator<UnifiableType> order = new Comparator<UnifiableType>() {
			@Override
			public int compare(UnifiableType o1, UnifiableType o2) {
				return o1.id().compareTo(o2.id());
			}
		};
		Collections.sort(roots, order);
		logger.debug("ROOTS: " + dag.roots());
		
		for (UnifiableType r : roots) {
			dag.postOrderFromWithOrder(new NodeWalker<UnifiableType>() {
				@Override
				public void present(Node<UnifiableType> node) {
					if (node.getEntry().isRedirected())
						return;
					node.getEntry().resolve(errors);
				}
			}, r, order);
		}
		
		// Then we can bind the types
		logger.debug("binding group:");
		for (Entry<TypeBinder, PosType> e : memberTypes.entrySet()) {
			Type as = cleanUTs(errors, e.getValue().pos, e.getValue().type, new ArrayList<>());
			logger.debug(e.getKey() + " :: " + as);
			TypeChecker.logger.info(e.getKey() + " :: " + as);
			e.getKey().bindType(as);
		}
		this.bindVarPatternTypes(errors);
	}

	private void expandUnions() {
		for (UnifiableType ut : allUTs) {
			ut.expandUnions();
		}
	}

	private void expandUsed() {
		for (UnifiableType ut : allUTs) {
			ut.expandUsed();
		}
	}

	private void mergePolyVars() {
		for (int i=0;i<allUTs.size();i++) {
			UnifiableType ut = allUTs.get(i);
			ut.mergePolyVars();
		}
	}

	private void acquireEquivalent() {
		List<UnifiableType> considered = new ArrayList<>();
		for (int i=0;i<allUTs.size();i++) {
			UnifiableType ut = allUTs.get(i);
			ut.acquireOthers(considered);
			considered.add(ut);
		}
	}

	private DirectedAcyclicGraph<UnifiableType> collate(ErrorReporter errors) {
		DirectedAcyclicGraph<UnifiableType> ret = new DirectedAcyclicGraph<>();
		for (int i=0;i<allUTs.size();i++) {
			UnifiableType ut = allUTs.get(i);
			if (ut.isRedirected()) {
				logger.debug("not collecting info on " + ut.id() + " because redirected to " + ut.redirectedTo());
				continue;
			}
			ret.ensure(ut);
			ut.collectInfo(errors, ret);
		}
		return ret;
	}

	private Type cleanUTs(ErrorReporter errors, InputPosition pos, Type ty, List<UnifiableType> recs) {
		logger.debug("Cleaning " + ty + " " + ty.getClass());
//		if (ty instanceof EnsureListMessage)
//			((EnsureListMessage)ty).validate(errors);
		if (ty instanceof UnifiableType) {
			List<UnifiableType> dontUse = new ArrayList<>(recs);
			UnifiableType ut = (UnifiableType)ty;
			dontUse.add(ut);
			return cleanUTs(errors, pos, ut.resolvedTo(), dontUse);
		} else if (ty instanceof Apply) {
			Apply a = (Apply) ty;
			List<Type> tys = new ArrayList<>();
			for (Type t : a.tys) {
				if (recs.contains(t)) {
					errors.message(pos, "circular polymorphic type inferred");
					return new ErrorType();
				}
				tys.add(cleanUTs(errors, pos, t, recs));
			}
			return new Apply(tys);
		} else if (ty instanceof PolyInstance) {
			PolyInstance pi = (PolyInstance) ty;
			List<Type> polys = new ArrayList<>();
			for (Type t : pi.getPolys()) {
				if (recs.contains(t)) {
					errors.message(pos, "circular polymorphic type inferred");
					return new ErrorType();
				}
				polys.add(cleanUTs(errors, pos, t, recs));
			}
			return new PolyInstance(pi.location(), (NamedType) cleanUTs(errors, pos, pi.struct(), recs), polys);
		} else {
			return ty;
		}
	}

	@Override
	public void resolveAll(ErrorReporter errors, boolean hard) {
		while (true) {
			List<UnifiableType> list = new ArrayList<>(allUTs);
			for (UnifiableType ut : list) {
				ut.resolve(errors);
				logger.debug("resolved to " + ((TypeConstraintSet) ut).debugInfo());
			}
			if (list.size() == allUTs.size())
				return;
		}
	}

	@Override
	public PosType consolidate(InputPosition pos, List<PosType> types) {
		if (types.isEmpty())
			throw new NotImplementedException("Cannot handle consolidating no types");
		
		// If there's just 1, that's easy
		if (types.size() == 1)
			return types.get(0);
		
		// If they appear to be all the same, no probs; if any of them is error, return that
		int commonApply = -1;
		PosType ret = types.get(0);
		pos = ret.pos;
		boolean allMatch = true;
		for (PosType t : types) {
			// actual error
			if (t.type instanceof ErrorType)
				return t;
			// user specified "an error" ... ignore this as part of "bottom"
			if (t.type == LoadBuiltins.error) {
				continue;
			}
			if (ret.type == LoadBuiltins.error)
				ret = t;
			if (!(t.type instanceof UnifiableType))
				pos = t.pos;
			if (ret.type != t.type) {
				allMatch = false;
			}
			if (t.type instanceof Apply) {
				if (commonApply == -1)
					commonApply = ((Apply)t.type).argCount();
				else if (commonApply != ((Apply)t.type).argCount())
					throw new HaventConsideredThisException("we could be asked to unify different levels of apply, providing UTs are involved somewhere; if not, I think that has to be a type error");
			} else if (t.type instanceof UnifiableType)
				commonApply = 0;
		}
		if (allMatch)
			return ret;

		if (commonApply > 0) {
			List<Type> args = new ArrayList<>();
			for (int i=0;i<=commonApply;i++) {
				List<PosType> ai = new ArrayList<>();
				for (PosType pt : types) {
					ai.add(new PosType(pos, ((Apply)pt.type).get(i)));
				}
				args.add(consolidate(pos, ai).type);
			}
			return new PosType(pos, new Apply(args));
		}

		// OK, create a new UT and attach them all
		StringBuilder motive = new StringBuilder("consolidating");
		for (PosType t : types) {
			motive.append(" ");
			Type tt = t.type;
			if (tt instanceof UnifiableType)
				motive.append(((UnifiableType)tt).id());
			else
				motive.append(tt.signature());
		}
		UnifiableType ut = createUT(pos, motive.toString(), false);
		for (PosType t : types) {
			if (t.type instanceof Apply) {
				((TypeConstraintSet) ut).consolidatedApplication((Apply) t.type);
			} else
				ut.sameAs(t.pos, t.type);
		}
		return new PosType(pos, ut);
	}
	
	@Override
	public PosType collapse(InputPosition pos, Collection<PosType> types) {
		if (types.isEmpty())
			throw new NotImplementedException("Cannot handle consolidating no types");
		
		PosType ret = types.iterator().next();
		// If there's just 1, that's easy
		if (types.size() == 1)
			return ret;
		
		// If they appear to be all the same, no probs; if any of them is error, return that
		pos = ret.pos;
		boolean allMatch = true;
		Set<PosType> uts = new TreeSet<>(TypeConstraintSet.posNameComparator);
		Set<Type> others = new HashSet<>();
		for (PosType t : types) {
			if (t.type instanceof ErrorType)
				return t;
			if (t.type instanceof UnifiableType) {
				uts.add(new PosType(t.pos, ((UnifiableType) t.type).redirectedTo()));
			} else {
				if (t.pos != null)
					pos = t.pos;
				others.add(t.type);
			}
			if (ret.type != t.type) {
				allMatch = false;
			}
		}
		if (allMatch)
			return ret;
		else if (others.isEmpty() && uts.size() == 1)
			return uts.iterator().next();

		// OK, let the first UT acquire the others
		UnifiableType ut = (UnifiableType) uts.iterator().next().type;
		logger.debug("allowing " + ut.id() + " to collapse " + types);
		for (PosType t : types) {
			if (t.type instanceof Apply) {
				((TypeConstraintSet) ut).consolidatedApplication((Apply) t.type);
			} else if (t.type instanceof UnifiableType)
				ut.acquire((UnifiableType) t.type);
			else
				ut.sameAs(t.pos, t.type);
		}
		return new PosType(pos, ut);
	}

	@Override
	public void debugInfo(String when) {
		logger.debug("------ " + when + " # = " + allUTs.size());
		for (UnifiableType ut : allUTs) {
			TypeConstraintSet tcs = (TypeConstraintSet)ut;
			logger.debug(tcs.debugInfo());
		}
		logger.debug("======");
	}
}
