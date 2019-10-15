package org.flasck.flas.tc3;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.parsedForm.PolyType;
import org.flasck.flas.parsedForm.StructDefn;
import org.flasck.flas.parsedForm.StructField;
import org.flasck.flas.repository.LoadBuiltins;
import org.zinutils.exceptions.NotImplementedException;

public class TypeConstraintSet implements UnifiableType {
	private final CurrentTCState state;
	private final Set<Type> incorporatedBys = new HashSet<>();
	private final Map<StructDefn, StructTypeConstraints> constraints = new TreeMap<>(StructDefn.nameComparator);
	private final InputPosition pos;
	private Type resolvedTo;
	private int returned = 0;
	
	public TypeConstraintSet(CurrentTCState state, InputPosition pos) {
		this.state = state;
		this.pos = pos;
	}

	@Override
	public Type resolve() {
		if (resolvedTo != null)
			return resolvedTo;
		if (constraints.isEmpty() && incorporatedBys.isEmpty() && returned == 0)
			return LoadBuiltins.any;
		if (!constraints.isEmpty()) {
			// We have been explicitly told that this is true, usually through pattern matching
			if (constraints.size() == 1) {
				StructDefn ty = constraints.keySet().iterator().next();
				if (!ty.hasPolys())
					return ty;
				else {
					StructTypeConstraints stc = constraints.get(ty);
					Map<PolyType, Type> polyMap = new HashMap<>();
					for (StructField f : stc.fields()) {
						PolyType pt = ty.findPoly(f.type);
						if (pt == null)
							continue;
						polyMap.put(pt, stc.get(f).resolve());
					}
					List<Type> polys = new ArrayList<>();
					for (PolyType p : ty.polys()) {
						if (polyMap.containsKey(p))
							polys.add(polyMap.get(p));
						else
							polys.add(LoadBuiltins.any);
					}
					resolvedTo = new PolyInstance(ty, polys);
					return resolvedTo;
				}
			}
		}
		if (incorporatedBys.isEmpty())
			resolvedTo = state.nextPoly(pos);
		else {
			// TODO: merge multiple things or throw an error
			resolvedTo = incorporatedBys.iterator().next();
		}
		return resolvedTo;
	}
	
	
	@Override
	public void isReturned() {
		returned ++;
	}

	@Override
	public void incorporatedBy(InputPosition pos, Type incorporator) {
		incorporatedBys.add(incorporator);
	}

	@Override
	public String signature() {
		if (resolvedTo == null)
			throw new NotImplementedException("Has not been resolved");
		return resolvedTo.signature();
	}

	@Override
	public int argCount() {
		if (resolvedTo == null)
			throw new NotImplementedException("Has not been resolved");
		return resolvedTo.argCount();
	}

	@Override
	public Type get(int pos) {
		if (resolvedTo == null)
			throw new NotImplementedException("Has not been resolved");
		return resolvedTo.get(pos);
	}

	@Override
	public boolean incorporates(Type other) {
		throw new NotImplementedException("The type algorithm should recognize us and call incorporatedBy instead");
	}

	@Override
	public StructTypeConstraints canBeStruct(StructDefn sd) {
		if (!constraints.containsKey(sd))
			constraints.put(sd, new StructFieldConstraints(sd));
		return constraints.get(sd);
	}
}
