package org.flasck.flas.tc3;

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.commonBase.names.NameOfThing;
import org.flasck.flas.commonBase.names.NamedThing;
import org.flasck.flas.commonBase.names.SolidName;
import org.flasck.flas.parsedForm.ContractDecl;
import org.flasck.flas.parsedForm.PolyHolder;
import org.flasck.flas.parsedForm.PolyType;
import org.flasck.flas.repository.RepositoryEntry;
import org.zinutils.exceptions.NotImplementedException;

// Tuple stores a collection of (different) types as if it were a polymorphic struct
public class Tuple implements PolyHolder, RepositoryEntry, NamedType, NamedThing {
	private final InputPosition loc;
	private final SolidName name;
	private final List<PolyType> polys = new ArrayList<>();

	// Note: there is the possibility here that multiple tuples will have the same name,
	// but I don't think we use or reference it anywhere, so that is not a problem.
	public Tuple(InputPosition loc, NameOfThing inside, int nargs) {
		this.loc = loc;
		this.name = new SolidName(inside, "_tuple_"+nargs);
		for (int i=0;i<nargs;i++) {
			polys.add(new PolyType(loc, new SolidName(this.name, "P" + i)));
		}
	}

	@Override
	public InputPosition location() {
		return loc;
	}

	@Override
	public NameOfThing getName() {
		return name;
	}

	@Override
	public String signature() {
		return ((SolidName)this.name()).baseName();
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
	public NameOfThing name() {
		return getName();
	}
	
	@Override
	public boolean hasPolys() {
		return true;
	}

	@Override
	public List<PolyType> polys() {
		return polys;
	}

	@Override
	public void dumpTo(PrintWriter pw) {
		pw.print(signature());
	}
	
	@Override
	public String toString() {
		return "Primitive[" + name().uniqueName() + "]";
	}

	@Override
	public boolean incorporates(InputPosition pos, Type other) {
		if (other instanceof UnifiableType) {
			((UnifiableType)other).incorporatedBy(pos, this);
			return true;
		}
		if (other instanceof Tuple && ((Tuple)other).name().uniqueName().equals(name().uniqueName()))
			return true;
		if (this.name.uniqueName().equals("Any"))
			return true;
		if (this.name.uniqueName().equals("Contract") && other instanceof ContractDecl)
			return true;
		return false;
	}
}
