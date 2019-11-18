package org.flasck.flas.tc3;

import java.io.PrintWriter;
import java.util.List;

import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.commonBase.names.NameOfThing;
import org.flasck.flas.repository.LoadBuiltins;
import org.flasck.flas.repository.RepositoryEntry;
import org.zinutils.exceptions.NotImplementedException;

public class PolyInstance implements NamedType, RepositoryEntry {
	private final NamedType ty;
	private final List<Type> polys;

	public PolyInstance(NamedType ty, List<Type> polys) {
		this.ty = ty;
		this.polys = polys;
	}

	// TODO: its possible for this to be a union as well, I think ...
	public NamedType struct() {
		return ty;
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
	public boolean incorporates(InputPosition pos, Type other) {
		if (!(other instanceof PolyInstance))
			return false;
		PolyInstance o = (PolyInstance) other;
		if (!this.ty.incorporates(pos, o.ty))
			return false;
		for (int i=0;i<polys.size();i++)
			if (polys.get(i) != LoadBuiltins.any && polys.get(i) != o.polys.get(i))
				return false;
		return true;
	}

	@Override
	public NameOfThing name() {
		return ty.name();
	}

	@Override
	public void dumpTo(PrintWriter pw) {
		throw new NotImplementedException();
	}

	@Override
	public String toString() {
		return ty.name().uniqueName() + polys;
	}
}
