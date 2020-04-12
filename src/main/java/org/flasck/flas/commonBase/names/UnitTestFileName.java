package org.flasck.flas.commonBase.names;

import org.zinutils.exceptions.NotImplementedException;
import org.zinutils.exceptions.UtilException;
import org.zinutils.xml.XMLElement;

public class UnitTestFileName implements NameOfThing {
	private final PackageName container;
	private final String name;

	public UnitTestFileName(PackageName container, String s) {
		this.container = container;
		this.name = s;
	}
	
	@Override
	public NameOfThing container() {
		return container;
	}
	
	@Override
	public PackageName packageName() {
		throw new NotImplementedException();
	}

	@Override
	public NameOfThing containingCard() {
		return null;
	}
	
	public String uniqueName() {
		return container.uniqueName() + "." + name;
	}
	
	@Override
	public String jsName() {
		return container.uniqueName() + "." + name;
	}

	@Override
	public String jsUName() {
		throw new UtilException("I don't think so");
	}
	
	@Override
	public String javaName() {
		return container.uniqueName() + "." + name;
	}

	@Override
	public String javaPackageName() {
		return container.uniqueName();
	}

	@Override
	public String javaClassName() {
		return javaName();
	}

	public String baseName() {
		return name;
	}
	
	@Override
	public String writeToXML(XMLElement xe) {
		XMLElement ty = xe.addElement("UnitTestFile");
		ty.setAttribute("container", container.uniqueName());
		ty.setAttribute("name", name);
		return name;
	}

	@Override
	public boolean equals(Object obj) {
		if (!(obj instanceof UnitTestFileName))
			return false;
		UnitTestFileName other = (UnitTestFileName) obj;
		if (!other.container.equals(container))
			return false;
		return name.equals(other.name);
	}
	
	@Override
	public int hashCode() {
		return container.hashCode() ^ name.hashCode();
	}

	@Override
	public <T extends NameOfThing> int compareTo(T obj) {
		UnitTestFileName other = (UnitTestFileName) obj;
		int v1 = container.compareTo(other.container);
		if (v1 != 0)
			return v1;
		return name.compareTo(other.name);
	}
}
