package org.flasck.flas.parsedForm;

import java.util.Arrays;
import java.util.List;

import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.commonBase.Expr;
import org.flasck.flas.repository.RepositoryReader;
import org.flasck.flas.tc3.Apply;
import org.flasck.flas.tc3.NamedType;
import org.flasck.flas.tc3.Type;
import org.zinutils.exceptions.CantHappenException;
import org.zinutils.exceptions.HaventConsideredThisException;
import org.zinutils.exceptions.UtilException;

public class TypeReference implements Expr {
	private InputPosition location;
	private String name;
	private List<TypeReference> polys;
	private NamedType definition;
	private Apply applyDefn;
	private boolean dynamic;

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

	public TypeReference bind(Type ty) {
		if (dynamic)
			throw new CantHappenException("you cannot bind a dynamic type directly");
		if (ty instanceof NamedType)
			definition = (NamedType) ty;
		else if (ty instanceof Apply)
			applyDefn = (Apply)ty;
		return this;
	}

	public Type defn() {
		if (dynamic)
			throw new CantHappenException("you need to access dynamic type references with resolveType");
		if (definition != null)
			return definition;
		else if (applyDefn != null)
			return applyDefn;
		else
			throw new CantHappenException("no type definition");
	}
	
	public NamedType namedDefn() {
		if (dynamic)
			throw new CantHappenException("you need to access dynamic type references with resolveType");
		if (definition == null && applyDefn != null)
			throw new CantHappenException("you need to call applyDefn for this");
		return definition;
	}

	public Apply applyDefn() {
		if (dynamic)
			throw new CantHappenException("you need to access dynamic type references with resolveType");
		if (definition != null && applyDefn == null)
			throw new CantHappenException("you need to call defn for this");
		return applyDefn;
	}

	public boolean isDynamic() {
		return dynamic;
	}

	public TypeReference bindDynamically() {
		this.dynamic = true;
		return this;
	}

	public Type resolveType(RepositoryReader repository) {
		if (!dynamic) {
			throw new CantHappenException("no, this should be bound statically");
		}
		if (!polys.isEmpty())
			throw new HaventConsideredThisException("resolving type with polys");
		return repository.get(name);
	}
}
