package org.flasck.flas.parsedForm;

import java.util.ArrayList;
import java.util.List;

import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.commonBase.Locatable;
import org.flasck.flas.commonBase.names.SolidName;
import org.zinutils.collections.CollectionUtils;

public class UnionTypeDefn implements Locatable {
	public final transient boolean generate;
	private final InputPosition location;
	private final SolidName name;
	public final List<TypeReference> cases = new ArrayList<TypeReference>();
	private List<PolyType> polyvars;

	public UnionTypeDefn(InputPosition location, boolean generate, SolidName defining, PolyType... polyvars) {
		this(location, generate, defining, CollectionUtils.listOf(polyvars));
	}
	
	public UnionTypeDefn(InputPosition location, boolean generate, SolidName defining, List<PolyType> polyvars) {
		this.generate = generate;
		this.location = location;
		this.name = defining;
		this.polyvars = polyvars;
	}
	
	public SolidName myName() {
		return name;
	}
	
	public String name() {
		return name.uniqueName();
	}
	
	public UnionTypeDefn addCase(TypeReference tr) {
		this.cases.add(tr);
		return this;
	}
	
	@Override
	public String toString() {
		return name();
	}

	@Override
	public InputPosition location() {
		return location;
	}

	public List<PolyType> polys() {
		return polyvars;
	}
}
