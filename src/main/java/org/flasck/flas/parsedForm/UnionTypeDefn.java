package org.flasck.flas.parsedForm;

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.commonBase.Locatable;
import org.flasck.flas.commonBase.names.SolidName;
import org.flasck.flas.parser.UnionFieldConsumer;
import org.flasck.flas.repository.RepositoryEntry;

public class UnionTypeDefn implements Locatable, UnionFieldConsumer, RepositoryEntry {
	public final transient boolean generate;
	private final InputPosition location;
	private final SolidName name;
	public final List<TypeReference> cases = new ArrayList<TypeReference>();
	private List<PolyType> polyvars;

	public UnionTypeDefn(InputPosition location, boolean generate, SolidName defining, PolyType... polyvars) {
		this(location, generate, defining, Arrays.asList(polyvars));
	}
	
	public UnionTypeDefn(InputPosition location, boolean generate, SolidName defining, List<PolyType> polyvars) {
		this.generate = generate;
		this.location = location;
		this.name = defining;
		this.polyvars = polyvars;
	}
	
	public SolidName name() {
		return name;
	}
	
	public UnionTypeDefn addCase(TypeReference tr) {
		this.cases.add(tr);
		return this;
	}
	
	@Override
	public String toString() {
		return name.uniqueName();
	}

	@Override
	public InputPosition location() {
		return location;
	}

	public List<PolyType> polys() {
		return polyvars;
	}

	@Override
	public void dumpTo(PrintWriter pw) {
		pw.println("Union[" + toString() + "]");
	}
}
