package org.flasck.flas.tc3;

import java.util.List;

import org.flasck.flas.parsedForm.StructDefn;
import org.zinutils.exceptions.NotImplementedException;

public class PolyInstance implements Type {
	private final StructDefn ty;
	private final List<Type> polys;

	public PolyInstance(StructDefn ty, List<Type> polys) {
		this.ty = ty;
		this.polys = polys;
	}

	public List<Type> getPolys() {
		return polys;
	}

	@Override
	public String signature() {
		StringBuilder ret = new StringBuilder();
		ret.append(ty.name().uniqueName());
		ret.append("[");
		String sep = "";
		for (Type p : polys) {
			ret.append(sep);
			ret.append(p.signature());
			sep = ",";
		}
		ret.append("]");
		return ret.toString();
	}

	@Override
	public int argCount() {
		return 0;
	}

	@Override
	public Type get(int pos) {
		throw new NotImplementedException();
	}

	@Override
	public boolean incorporates(Type other) {
		throw new NotImplementedException();
	}

	@Override
	public String toString() {
		return ty.name().uniqueName() + polys;
	}
}
