package org.flasck.flas.commonBase.names;

import org.zinutils.exceptions.UtilException;
import org.zinutils.xml.XMLElement;

public class PackageName implements NameOfThing, Comparable<PackageName> {
	private final String name;

	public PackageName(String s) {
		this.name = s;
	}
	
	@Override
	public NameOfThing containingCard() {
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
	
	@Override
	public String javaName() {
		return name;
	}

	@Override
	public String javaPackageName() {
		return name;
	}

	@Override
	public String javaClassName() {
		return name;
	}

	public String simpleName() {
		return name;
	}

	public String finalPart() {
		int idx = name.lastIndexOf('.');
		if (idx == -1)
			return name;
		else
			return name.substring(idx+1);
	}

	@Override
	public String writeToXML(XMLElement xe) {
		XMLElement ty = xe.addElement("Package");
		ty.setAttribute("name", name);
		return name;
	}

	@Override
	public boolean equals(Object obj) {
		return obj instanceof PackageName && name.equals(((PackageName)obj).name);
	}
	
	@Override
	public int hashCode() {
		return name.hashCode();
	}
	
	@Override
	public int compareTo(PackageName o) {
		return name.compareTo(o.name);
	}
	
	@Override
	public <T extends NameOfThing> int compareTo(T other) {
		if (!(other instanceof PackageName))
			return other.getClass().getName().compareTo(this.getClass().getName());
		return this.compareTo((PackageName)other);
	}
}
