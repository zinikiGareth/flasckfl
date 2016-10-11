package org.flasck.flas.parsedForm;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.commonBase.Locatable;
import org.zinutils.collections.CollectionUtils;

@SuppressWarnings("serial")
public class UnionTypeDefn implements Serializable, Locatable {
	public final transient boolean generate;
	private final InputPosition location;
	private final String name;
	public final List<TypeReference> cases = new ArrayList<TypeReference>();
	private List<TypeReference> polyvars;

	public UnionTypeDefn(InputPosition location, boolean generate, String defining, TypeReference... polyvars) {
		this(location, generate, defining, CollectionUtils.listOf(polyvars));
	}
	
	public UnionTypeDefn(InputPosition location, boolean generate, String defining, List<TypeReference> polyvars) {
		this.generate = generate;
		this.location = location;
		this.name = defining;
		this.polyvars = polyvars;
	}
	
	public String name() {
		return name;
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

	public List<TypeReference> polys() {
		return polyvars;
	}
}
