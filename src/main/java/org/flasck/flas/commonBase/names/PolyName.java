package org.flasck.flas.commonBase.names;

import org.zinutils.exceptions.NotImplementedException;
import org.zinutils.exceptions.UtilException;
import org.zinutils.xml.XMLElement;

public class PolyName implements NameOfThing, Comparable<PolyName> {
	private final String name;

	public PolyName(String s) {
		this.name = s;
	}
	
	@Override
	public CardName containingCard() {
		return null;
	}
	
	public String uniqueName() {
		return name;
	}
	
	@Override
	public String jsName() {
		return name;
	}

	@Override
	public String jsUName() {
		throw new UtilException("I don't think so");
	}

	public String simpleName() {
		return name;
	}
	
	@Override
	public String javaName() {
		throw new NotImplementedException();
	}

	@Override
	public String javaClassName() {
		throw new NotImplementedException();
	}

	@Override
	public boolean equals(Object obj) {
		return obj instanceof PolyName && name.equals(((PolyName)obj).name);
	}
	
	@Override
	public int hashCode() {
		return name.hashCode();
	}
	
	@Override
	public int compareTo(PolyName o) {
		return name.compareTo(o.name);
	}
	
	@Override
	public <T extends NameOfThing> int compareTo(T other) {
		if (!(other instanceof PolyName))
			return other.getClass().getName().compareTo(this.getClass().getName());
		return this.compareTo((PolyName)other);
	}

	@Override
	public String writeToXML(XMLElement xe) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String javaPackageName() {
		throw new org.zinutils.exceptions.NotImplementedException();
	}
}
