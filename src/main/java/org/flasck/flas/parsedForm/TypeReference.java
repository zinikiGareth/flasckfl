package org.flasck.flas.parsedForm;

import java.util.List;

import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.commonBase.Locatable;
import org.zinutils.collections.CollectionUtils;

public class TypeReference implements Locatable {

	private InputPosition location;
	private String name;
	private List<TypeReference> polys;

	public TypeReference(InputPosition location, String name, TypeReference... polys) {
		this(location, name, CollectionUtils.listOf(polys));
	}

	public TypeReference(InputPosition location, String name, List<TypeReference> polys) {
		this.location = location;
		this.name = name;
		this.polys = polys;
	}

	public String name() {
		return name;
	}

	public InputPosition location() {
		return location;
	}

	public boolean hasPolys() {
		return polys != null && !polys.isEmpty();
	}

	public List<TypeReference> polys() {
		return polys;
	}

}
