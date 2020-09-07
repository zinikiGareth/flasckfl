package org.flasck.flas.tc3;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.commonBase.names.FunctionName;
import org.flasck.flas.commonBase.names.SolidName;
import org.flasck.flas.errors.ErrorMark;
import org.flasck.flas.errors.ErrorReporter;
import org.flasck.flas.parsedForm.IntroduceVar;
import org.flasck.flas.parsedForm.LogicHolder;
import org.flasck.flas.parsedForm.PolyType;
import org.flasck.flas.parsedForm.TypeBinder;
import org.flasck.flas.parsedForm.VarPattern;
import org.flasck.flas.repository.FunctionGroup;
import org.flasck.flas.repository.LoadBuiltins;
import org.flasck.flas.repository.RepositoryReader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.zinutils.collections.MapMap;
import org.zinutils.exceptions.CantHappenException;
import org.zinutils.exceptions.CycleDetectedException;
import org.zinutils.exceptions.HaventConsideredThisException;
import org.zinutils.exceptions.NotImplementedException;
import org.zinutils.graphs.DirectedAcyclicGraph;
import org.zinutils.graphs.Node;
import org.zinutils.graphs.NodeWalker;

public class FunctionGroupTCState implements CurrentTCState {
	private final static Logger uniflogger = LoggerFactory.getLogger("TCUnification");
	private final RepositoryReader repository;
	private final MapMap<String, String, UnifiableType> constraints = new MapMap<>();
	private final Map<VarPattern, UnifiableType> patts = new TreeMap<>(VarPattern.comparator);
	private final Map<IntroduceVar, UnifiableType> introductions = new TreeMap<>(IntroduceVar.comparator);
	int polyCount = 0;
	private List<UnifiableType> allUTs = new ArrayList<>();
	private final boolean hasGroup;
	private Map<String, Type> memberTypes = new TreeMap<>();
	
	public FunctionGroupTCState(RepositoryReader repository, FunctionGroup grp) {
		this.repository = repository;
		uniflogger.info("  binding return types for group");
		for (LogicHolder x : grp.functions())
			bindVarToUT(x.name().uniqueName(), x.name().uniqueName(), createUT(x.location(), x.name().uniqueName() + " returns", false));
		this.hasGroup = !grp.isEmpty();
	}

	@Override
	public void recordMember(FunctionName name, List<Type> ats) {
		if (ats.isEmpty())
			memberTypes.put(name.uniqueName(), requireVarConstraints(null, name.uniqueName(), name.uniqueName()));
		else
			memberTypes.put(name.uniqueName(), new Apply(ats, requireVarConstraints(null, name.uniqueName(), name.uniqueName())));
	}

	@Override
	public Type getMember(FunctionName name) {
		return memberTypes.get(name.uniqueName());
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
	public void bindVarToUT(String fnCxt, String name, UnifiableType ty) {
		if (!allUTs.contains(ty))
			throw new NotImplementedException("Where did this come from?");
		if (constraints.contains(fnCxt, name))
			throw new CantHappenException("duplicate name for one var: " + name + " in " + fnCxt);
		uniflogger.info("    binding " + fnCxt + " :: " + name + " to " + ty.id() + " for " + ty.motive());
		constraints.add(fnCxt, name, ty);
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
	public UnifiableType requireVarConstraints(InputPosition pos, String fnCxt, String var) {
		if (!constraints.contains(fnCxt, var))
			throw new RuntimeException("We don't have var constraints for " + var + " in " + fnCxt + " but it should have been bound during arg processing: " + constraints.key2Set(fnCxt));
		return constraints.get(fnCxt, var);
	}

	@Override
	public UnifiableType hasVar(String fnCxt, String var) {
		return constraints.get(fnCxt, var);
	}

	@Override
	public PolyType nextPoly(InputPosition pos) {
		if (polyCount >= 26)
			throw new NotImplementedException("Cannot handle more than 26 poly types at once");
		return new PolyType(pos, new SolidName(null, new String(new char[] { (char)('A' + polyCount++) })));
	}
	
	@Override
	public void groupDone(ErrorReporter errors, Map<TypeBinder, PosType> memberTypes, Map<TypeBinder, PosType> resultTypes) {
		ErrorMark mark = errors.mark();
		// TODO: should we use an ErrorMark so as to stop when errors occur and avoid cascades?
		TypeChecker.logger.debug("starting to unify types found in group: " + memberTypes.keySet());
		for (Entry<TypeBinder, PosType> e : memberTypes.entrySet()) {
			if (e.getValue() == null) {
				// something went wrong - probably just "do not generate"; anyway, we won't be able to do anything
				return;
			}
			uniflogger.debug("  " + e.getKey() + " :: " + e.getValue().type);
		}
		
		// When traversing the group, we should have managed to figure out some kind of type for it
		// Now use that to place constraints on the return type
		for (Entry<TypeBinder, PosType> m : resultTypes.entrySet()) {
			String name = m.getKey().name().uniqueName();
			UnifiableType ut = this.requireVarConstraints(m.getKey().location(), name, name);
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
		if (dag == null) { // cycle detected and error reported
			return;
		}
		uniflogger.debug("UT DAG:\n" + dag.toString());
		List<UnifiableType> roots = dag.roots();
		Comparator<UnifiableType> order = new Comparator<UnifiableType>() {
			@Override
			public int compare(UnifiableType o1, UnifiableType o2) {
				return o1.id().compareTo(o2.id());
			}
		};
		Collections.sort(roots, order);
		uniflogger.debug("ROOTS: " + dag.roots());
		
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
		
		if (!mark.hasMoreNow()) { // avoid cascades
			for (UnifiableType r : roots) {
				dag.postOrderFromWithOrder(new NodeWalker<UnifiableType>() {
					@Override
					public void present(Node<UnifiableType> node) {
						if (node.getEntry().isRedirected())
							return;
						node.getEntry().afterResolution(errors);
					}
				}, r, order);
			}
		}
		
		// Then we can bind the types
		uniflogger.debug("binding group:");
		for (Entry<TypeBinder, PosType> e : memberTypes.entrySet()) {
			Type as = cleanUTs(errors, e.getValue().pos, e.getValue().type, new ArrayList<>(), new HashMap<>());
			uniflogger.debug(e.getKey() + " :: " + as);
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
			ret.ensure(ut);
			if (ut.isRedirected()) {
				uniflogger.debug("not collecting info on " + ut.id() + " because redirected to " + ut.redirectedTo());
				ret.ensure(ut.redirectedTo());
				ret.ensureLink(ut, ut.redirectedTo());
				continue;
			}
			try {
				ut.collectInfo(errors, ret);
			} catch (CycleDetectedException ex) {
				errors.message(ut.location(), "cycle detected in type");
				return null;
			}
		}
		return ret;
	}

	private Type cleanUTs(ErrorReporter errors, InputPosition pos, Type ty, List<UnifiableType> recs, Map<PolyType, PolyType> inorderPolys) {
		uniflogger.debug("Cleaning " + ty + " " + ty.getClass());
//		if (ty instanceof EnsureListMessage)
//			((EnsureListMessage)ty).validate(errors);
		if (ty instanceof UnifiableType) {
			List<UnifiableType> dontUse = new ArrayList<>(recs);
			UnifiableType ut = (UnifiableType)ty;
			dontUse.add(ut);
			return cleanUTs(errors, pos, ut.resolvedTo(), dontUse, inorderPolys);
		} else if (ty instanceof Apply) {
			Apply a = (Apply) ty;
			List<Type> tys = new ArrayList<>();
			for (Type t : a.tys) {
				if (recs.contains(t)) {
					errors.message(pos, "circular polymorphic type inferred");
					return new ErrorType();
				}
				tys.add(cleanUTs(errors, pos, t, recs, inorderPolys));
			}
			return new Apply(tys);
		} else if (ty instanceof PolyInstance) {
			PolyInstance pi = (PolyInstance) ty;
			List<Type> polys = new ArrayList<>();
			for (Type t : pi.polys()) {
				if (recs.contains(t)) {
					errors.message(pos, "circular polymorphic type inferred");
					return new ErrorType();
				}
				polys.add(cleanUTs(errors, pos, t, recs, inorderPolys));
			}
			return new PolyInstance(pi.location(), (NamedType) cleanUTs(errors, pos, pi.struct(), recs, inorderPolys), polys);
		} else if (ty instanceof PolyType) {
			if (inorderPolys.containsKey(ty))
				return inorderPolys.get(ty);
			PolyType curr = (PolyType) ty;
			PolyType ret = new PolyType(curr.location(), new SolidName(curr.name().container(), new String(new char[] { (char)('A' + inorderPolys.size()) })));
			inorderPolys.put(curr, ret);
			return ret;
		} else {
			return ty;
		}
	}

	@Override
	public PosType consolidate(InputPosition pos, Collection<PosType> types) {
		if (types.isEmpty())
			throw new NotImplementedException("Cannot handle consolidating no types");
		
		// If there's just 1, that's easy
		PosType ret = types.iterator().next();
		if (types.size() == 1)
			return ret;
		
		// If they appear to be all the same, no probs; if any of them is error, return that
		int commonApply = -1;
		pos = ret.pos;
		boolean allMatch = true;
		boolean haveUTs = false;
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
					throw new HaventConsideredThisException("we could be asked to unify different levels of apply, providing UTs are involved somewhere; if not, I think that has to be a type error: " + types);
			} else if (t.type instanceof UnifiableType)
				haveUTs = true;
			else
				commonApply = 0;
		}
		if (allMatch)
			return ret;
		if (haveUTs)
			return collapse(pos, types);

		if (commonApply > 0) {
			List<Type> args = new ArrayList<>();
			for (int i=0;i<=commonApply;i++) {
				List<PosType> ai = new ArrayList<>();
				for (PosType pt : types) {
					if (pt.type instanceof Apply)
						ai.add(new PosType(pos, ((Apply)pt.type).get(i)));
				}
				PosType ct = consolidate(pos, ai);
				args.add(ct.type);
			}
			Apply c = new Apply(args);
			for (PosType pt : types) {
				if (pt.type instanceof UnifiableType)
					((UnifiableType)pt.type).recordApplication(pt.pos, args);
			}
			return new PosType(pos, c);
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
		uniflogger.debug("  " + motive + " as " + ut.id());
		for (PosType t : types) {
			if (t.type instanceof Apply) {
				((TypeConstraintSet) ut).consolidatedApplication(pos, (Apply) t.type);
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
		uniflogger.debug("  allowing " + ut.id() + " to collapse " + types);
		for (PosType t : types) {
			if (t.type instanceof Apply) {
				((TypeConstraintSet) ut).consolidatedApplication(t.pos, (Apply) t.type);
			} else if (t.type instanceof UnifiableType)
				ut.acquire((UnifiableType) t.type);
			else
				ut.sameAs(t.pos, t.type);
		}
		return new PosType(pos, ut);
	}

	@Override
	public void debugInfo(String when) {
		uniflogger.debug("------ " + when + " # = " + allUTs.size());
		for (UnifiableType ut : allUTs) {
			TypeConstraintSet tcs = (TypeConstraintSet)ut;
			uniflogger.debug(tcs.debugInfo());
		}
		uniflogger.debug("======");
	}
}
