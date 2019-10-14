package org.flasck.flas.rewrittenForm;

import java.util.ArrayList;
import java.util.List;

import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.commonBase.names.NameOfThing;
import org.flasck.flas.types.PolyVar;
import org.flasck.flas.types.Type;
import org.flasck.flas.types.TypeWithName;
import org.flasck.flas.types.TypeWithNameAndPolys;
import org.zinutils.exceptions.UtilException;

public class RWUnionTypeDefn extends TypeWithNameAndPolys implements Comparable<RWUnionTypeDefn> {
	public final transient boolean generate;
	public final List<TypeWithName> cases = new ArrayList<>();

	public RWUnionTypeDefn(InputPosition location, boolean generate, NameOfThing defining, List<PolyVar> polyvars) {
		super(null, location, defining, polyvars);
		this.generate = generate;
	}
	
	public RWUnionTypeDefn addCase(TypeWithName tr) {
		this.cases.add(tr);
		return this;
	}
	
	public boolean hasCtor(String s) {
		for (TypeWithName cs : cases)
			if (cs.nameAsString().equals(s))
				return true;
		return false;
	}

	// Offer a mapping of a child's polymorphic args onto ours.
	// e.g. for MyUnion[A,B,C] = OptA[A} | OptB[B]
	// get(A) = [0], get(B) = [1]
	public List<Integer> getCtorPolyArgPosns(String name) {
		if (!hasPolys())
			return new ArrayList<Integer>();
		for (TypeWithName cs : cases) {
			if (cs.nameAsString().equals(name)) {
				List<Integer> ret = new ArrayList<>();
				if (cs instanceof TypeWithNameAndPolys && ((TypeWithNameAndPolys)cs).hasPolys()) {
					for (Type t : ((TypeWithNameAndPolys)cs).polys()) {
						int k = -1;
						for (int i=0;i<polys().size();i++) {
							if (polys().get(i).equals(t))
								k = i;
						}
						if (k == -1)
							throw new UtilException("Type " + nameAsString() + " does not have poly " + t + " from case " + name);
						ret.add(k);
					}
				}
				return ret;
			}
		}
		throw new UtilException("The union " + nameAsString() + " does not contain " + name);
	}

	@Override
	public int compareTo(RWUnionTypeDefn o) {
		return nameAsString().compareTo(o.nameAsString());
	}
	
	@Override
	public String toString() {
		return nameAsString();
	}
}
