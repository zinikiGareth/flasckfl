package org.flasck.flas.parsedForm;

import java.util.Arrays;
import java.util.List;

import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.commonBase.Expr;
import org.flasck.flas.tc3.NamedType;
import org.zinutils.exceptions.UtilException;

public class TypeReference implements Expr {
	private InputPosition location;
	private String name;
	private List<TypeReference> polys;
	private NamedType definition;

	public TypeReference(InputPosition location, String name, TypeReference... polys) {
		this(location, name, Arrays.asList(polys));
	}

	public TypeReference(InputPosition location, String name, List<TypeReference> polys) {
		if (location == null)
			throw new UtilException("Null location in typereference");
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

	@Override
	public String toString() {
		return name + (polys!= null && !polys.isEmpty()?polys:"");
	}

	public TypeReference bind(NamedType ty) {
		definition = ty;
		return this;
	}

	public NamedType defn() {
		return definition;
	}
}
