package org.flasck.flas.rewrittenForm;

import java.util.ArrayList;
import java.util.List;

import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.typechecker.Type;
import org.zinutils.exceptions.UtilException;

public class RWUnionTypeDefn extends Type implements Comparable<RWUnionTypeDefn> {
	public final transient boolean generate;
	public final List<Type> cases = new ArrayList<Type>();

	public RWUnionTypeDefn(InputPosition location, boolean generate, String defining, List<Type> polyvars) {
		super(null, location, WhatAmI.UNION, defining, polyvars);
		this.generate = generate;
	}
	
	public RWUnionTypeDefn addCase(Type tr) {
		this.cases.add(tr);
		return this;
	}
	
	public boolean hasCtor(String s) {
		for (Type cs : cases)
			if (cs.name().equals(s))
				return true;
		return false;
	}

	// Offer a mapping of a child's polymorphic args onto ours.
	// e.g. for MyUnion[A,B,C] = OptA[A} | OptB[B]
	// get(A) = [0], get(B) = [1]
	public List<Integer> getCtorPolyArgPosns(String name) {
		if (!hasPolys())
			return new ArrayList<Integer>();
		for (Type cs : cases) {
			if (cs.name().equals(name)) {
				List<Integer> ret = new ArrayList<>();
				if (cs.hasPolys()) {
					for (Type t : cs.polys()) {
						int k = -1;
						for (int i=0;i<polys().size();i++) {
							if (polys().get(i).equals(t))
								k = i;
						}
						if (k == -1)
							throw new UtilException("Type " + name() + " does not have poly " + t + " from case " + name);
						ret.add(k);
					}
				}
				return ret;
			}
		}
		throw new UtilException("The union " + name() + " does not contain " + name);
	}

	@Override
	public int compareTo(RWUnionTypeDefn o) {
		return name.compareTo(o.name);
	}
	
	@Override
	public String toString() {
		return name();
	}
}
