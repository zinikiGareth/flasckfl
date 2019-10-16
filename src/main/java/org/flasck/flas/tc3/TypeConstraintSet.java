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
	private final Map<StructDefn, StructTypeConstraints> ctors = new TreeMap<>(StructDefn.nameComparator);
	private final Set<Type> types = new HashSet<>();
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
		if (ctors.isEmpty() && incorporatedBys.isEmpty() && types.isEmpty() && returned == 0)
			return LoadBuiltins.any;
		if (!types.isEmpty()) {
			if (types.size() == 1) {
				Type ret = types.iterator().next();
				if (ret instanceof StructDefn && ((StructDefn)ret).hasPolys()) {
					StructDefn sd = (StructDefn) ret;
					List<Type> polys = new ArrayList<>();
					for (PolyType p : sd.polys()) {
						polys.add(LoadBuiltins.any);
					}
					return new PolyInstance(sd, polys);
				}
				return ret;
			}
			throw new NotImplementedException("a unification case");
		}
		if (!ctors.isEmpty()) {
			// We have been explicitly told that this is true, usually through pattern matching
			if (ctors.size() == 1) {
				StructDefn ty = ctors.keySet().iterator().next();
				if (!ty.hasPolys())
					return ty;
				else {
					StructTypeConstraints stc = ctors.get(ty);
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
		if (!ctors.containsKey(sd))
			ctors.put(sd, new StructFieldConstraints(sd));
		return ctors.get(sd);
	}

	@Override
	public void canBeType(Type ofType) {
		types.add(ofType);
	}
}
