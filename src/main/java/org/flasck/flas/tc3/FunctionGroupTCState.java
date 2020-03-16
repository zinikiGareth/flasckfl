package org.flasck.flas.tc3;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;

import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.errors.ErrorReporter;
import org.flasck.flas.parsedForm.PolyType;
import org.flasck.flas.parsedForm.StandaloneDefn;
import org.flasck.flas.parsedForm.VarPattern;
import org.flasck.flas.repository.FunctionGroup;
import org.flasck.flas.repository.RepositoryReader;
import org.zinutils.exceptions.NotImplementedException;

public class FunctionGroupTCState implements CurrentTCState {
	private final RepositoryReader repository;
	private final Map<String, UnifiableType> constraints = new TreeMap<>();
	private final Map<VarPattern, UnifiableType> patts = new TreeMap<>(VarPattern.comparator);
	int polyCount = 0;
	private Set<UnifiableType> allUTs = new LinkedHashSet<>();
	private final boolean hasGroup;
	
	public FunctionGroupTCState(RepositoryReader repository, FunctionGroup grp) {
		this.repository = repository;
		for (StandaloneDefn x : grp.functions())
			bindVarToUT(x.name().uniqueName(), createUT(x.location(), "introducing " + x.name().uniqueName()));
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
	public void bindVarPatternTypes(ErrorReporter errors) {
		for (Entry<VarPattern, UnifiableType> e : patts.entrySet()) {
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
		return new PolyType(pos, new String(new char[] { (char)('A' + polyCount++) }));
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
	public void debugInfo() {
		System.out.println("------");
		for (UnifiableType ut : allUTs) {
			TypeConstraintSet tcs = (TypeConstraintSet)ut;
			System.out.println(tcs.debugInfo());
		}
		System.out.println("======");
	}
}
