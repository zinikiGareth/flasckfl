package org.flasck.flas.tc3;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;

import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.commonBase.names.SolidName;
import org.flasck.flas.errors.ErrorReporter;
import org.flasck.flas.parsedForm.IntroduceVar;
import org.flasck.flas.parsedForm.PolyType;
import org.flasck.flas.parsedForm.StandaloneDefn;
import org.flasck.flas.parsedForm.TypeBinder;
import org.flasck.flas.parsedForm.VarPattern;
import org.flasck.flas.repository.FunctionGroup;
import org.flasck.flas.repository.RepositoryReader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.zinutils.exceptions.NotImplementedException;

public class FunctionGroupTCState implements CurrentTCState {
	private final static Logger logger = LoggerFactory.getLogger("TCUnification");
	private final RepositoryReader repository;
	private final Map<String, UnifiableType> constraints = new TreeMap<>();
	private final Map<VarPattern, UnifiableType> patts = new TreeMap<>(VarPattern.comparator);
	private final Map<IntroduceVar, UnifiableType> introductions = new TreeMap<>(IntroduceVar.comparator);
	int polyCount = 0;
	private Set<UnifiableType> allUTs = new LinkedHashSet<>();
	private final boolean hasGroup;
	
	public FunctionGroupTCState(RepositoryReader repository, FunctionGroup grp) {
		this.repository = repository;
		for (StandaloneDefn x : grp.functions())
			bindVarToUT(x.name().uniqueName(), createUT(x.location(), "return value of " + x.name().uniqueName()));
		this.hasGroup = !grp.isEmpty();
	}

	@Override
	public boolean hasGroup() {
		return hasGroup;
	}
	
	@Override
	public UnifiableType createUT(InputPosition pos, String motive) {
		TypeConstraintSet ret = new TypeConstraintSet(repository, this, pos, "ret_" + allUTs.size(), motive);
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
			if (!ut.isResolved())
				throw new RuntimeException("Not yet resolved");
			e.getKey().bindType(ut.resolve(errors, true));
		}
	}

	@Override
	public void bindIntroducedVarTypes(ErrorReporter errors) {
		for (Entry<IntroduceVar, UnifiableType> e : introductions.entrySet()) {
			UnifiableType ut = e.getValue();
			if (!ut.isResolved())
				throw new RuntimeException("Not yet resolved");
			e.getKey().bindType(ut.resolve(errors, true));
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

		TypeChecker.logger.info("starting to check group: " + memberTypes.keySet());
		for (Entry<TypeBinder, PosType> e : memberTypes.entrySet())
			logger.debug(e.getKey() + " :: " + e.getValue().type);
		
		// if we picked up anything based on the invocation of the method in this group, add that into the mix
		for (Entry<TypeBinder, PosType> m : memberTypes.entrySet()) {
			String name = m.getKey().name().uniqueName();
			UnifiableType ut = this.requireVarConstraints(m.getKey().location(), name);
			ut.determinedType(m.getValue());
		}
		this.debugInfo("initial");

		// Then we can resolve all the UTs
		this.resolveAll(errors, false);
		this.enhanceAllMutualUTs();
		this.debugInfo("soft");
//		this.debugInfo();
		this.resolveAll(errors, true);
//		this.debugInfo();
		
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

	private Type cleanUTs(ErrorReporter errors, InputPosition pos, Type ty, List<UnifiableType> recs) {
		logger.debug("Cleaning " + ty + " " + ty.getClass());
		if (ty instanceof EnsureListMessage)
			((EnsureListMessage)ty).validate(errors);
		if (ty instanceof UnifiableType) {
			List<UnifiableType> dontUse = new ArrayList<>(recs);
			UnifiableType ut = (UnifiableType)ty;
			dontUse.add(ut);
			return cleanUTs(errors, pos, ut.resolve(errors, true), dontUse);
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
				ut.resolve(errors, hard);
			}
			if (list.size() == allUTs.size())
				return;
		}
	}

	@Override
	public void enhanceAllMutualUTs() {
		while (true) {
			boolean again = false;
			for (UnifiableType ut : allUTs) {
				again |= ut.enhance();
			}
			if (!again)
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
		PosType ret = types.get(0);
		pos = ret.pos;
		boolean allMatch = true;
		for (PosType t : types) {
			if (t.type instanceof ErrorType)
				return t;
			if (!(t.type instanceof UnifiableType))
				pos = ret.pos;
			if (ret.type != t.type) {
				allMatch = false;
			}
		}
		if (allMatch)
			return ret;

		// OK, create a new UT and attach them all
		UnifiableType ut = createUT(pos, "consolidating " + types);
		for (PosType t : types) {
			if (t.type instanceof Apply) {
				((TypeConstraintSet) ut).consolidatedApplication((Apply) t.type);
			} else
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
