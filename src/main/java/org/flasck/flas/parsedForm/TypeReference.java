package org.flasck.flas.parsedForm;

import java.util.Arrays;
import java.util.List;

import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.commonBase.Locatable;
import org.flasck.flas.commonBase.names.NameOfThing;
import org.zinutils.exceptions.UtilException;
import org.zinutils.xml.XMLElement;

public class TypeReference implements Locatable, NameOfThing {
	private InputPosition location;
	private String name;
	private List<TypeReference> polys;

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

	@Override
	public String uniqueName() {
		throw new org.zinutils.exceptions.NotImplementedException();
	}

	@Override
	public String jsName() {
		throw new org.zinutils.exceptions.NotImplementedException();
	}

	@Override
	public String jsUName() {
		throw new org.zinutils.exceptions.NotImplementedException();
	}

	@Override
	public String javaName() {
		throw new org.zinutils.exceptions.NotImplementedException();
	}

	@Override
	public String javaPackageName() {
		throw new org.zinutils.exceptions.NotImplementedException();
	}

	@Override
	public String javaClassName() {
		throw new org.zinutils.exceptions.NotImplementedException();
	}

	@Override
	public NameOfThing containingCard() {
		throw new org.zinutils.exceptions.NotImplementedException();
	}

	@Override
	public <T extends NameOfThing> int compareTo(T other) {
		throw new org.zinutils.exceptions.NotImplementedException();
	}

	@Override
	public String writeToXML(XMLElement xe) {
		throw new org.zinutils.exceptions.NotImplementedException();
	}
}
