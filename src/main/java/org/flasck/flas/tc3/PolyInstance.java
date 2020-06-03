package org.flasck.flas.tc3;

import java.io.PrintWriter;
import java.util.Iterator;
import java.util.List;

import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.commonBase.names.NameOfThing;
import org.flasck.flas.parsedForm.StructDefn;
import org.flasck.flas.parsedForm.UnionTypeDefn;
import org.flasck.flas.repository.RepositoryEntry;
import org.zinutils.exceptions.CantHappenException;
import org.zinutils.exceptions.NotImplementedException;

public class PolyInstance implements NamedType, RepositoryEntry {
	private final InputPosition loc;
	private final NamedType ty;
	private final List<Type> polys;

	public PolyInstance(InputPosition loc, NamedType ty, List<Type> polys) {
		if (ty instanceof PolyInstance)
			throw new CantHappenException("Don't wrap a poly in a poly");
		this.loc = loc;
		this.ty = ty;
		this.polys = polys;
	}


	@Override
	public boolean equals(Object obj) {
		if (!(obj instanceof PolyInstance))
			return false;
		PolyInstance o = (PolyInstance) obj;
		if (o.struct() != this.struct())
			return false;
		Iterator<Type> mine = polys.iterator();
		Iterator<Type> other = o.polys.iterator();
		while (mine.hasNext())
			if (!mine.next().equals(other.next()))
				return false;
		return true;
	}
	
	@Override
	public int hashCode() {
		return ty.hashCode();
	}
	
	public NamedType struct() {
		return ty;
	}
	
	public List<Type> getPolys() {
		return polys;
	}

	@Override
	public InputPosition location() {
		return loc;
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
		if (pos == 0)
			return this;
		else
			throw new NotImplementedException();
	}

	@Override
	public boolean incorporates(InputPosition pos, Type other) {
		if (other instanceof PolyInstance) {
			PolyInstance o = (PolyInstance) other;
			if (!this.ty.incorporates(pos, o.ty))
				return false;
			for (int i=0;i<polys.size();i++)
				if (!polys.get(i).incorporates(pos, o.polys.get(i)))
					return false;
			return true;
		} else if (other instanceof UnifiableType) {
			((UnifiableType) other).incorporatedBy(pos, this);
			return true;
		} else if (this.ty == other) {
			return true;
		} else if (this.ty instanceof UnionTypeDefn && other instanceof StructDefn && !((StructDefn)other).hasPolys()) {
			// it is acceptable for it to be incorporated if it is a non-poly version of a non-poly constructor in a union
			// Think Nil in List[A]
			return this.ty.incorporates(pos, other);
		} else {
			return false;
		}
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
