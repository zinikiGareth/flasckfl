package org.flasck.flas.commonBase.names;

import org.flasck.jvm.J;
import org.zinutils.xml.XMLElement;

public class SolidName implements NameOfThing, Comparable<SolidName> {
	private final NameOfThing container;
	private final String name;

	public SolidName(NameOfThing container, String name) {
		this.container = container;
		this.name = name;
	}

	public String baseName() {
		return name;
	}
	
	public String uniqueName() {
		if (container == null || container.uniqueName() == null || container.uniqueName().length() == 0)
			return name;
		else
			return container.uniqueName() + "." + name;
	}
	
	@Override
	public String jsUName() {
		return container.jsName() + "._" + name;
	}
	
	@Override
	public String jsName() {
		if (container == null || container.jsName() == null || container.jsName().length() == 0)
			return name;
		return container.jsName() + "." + name;
	}
	
	@Override
	public String javaName() {
		return uniqueName();
	}

	@Override
	public String javaClassName() {
		if (container == null)
			return J.BUILTINPKG + "." + name;
		else
			return container.uniqueName() + "$" + name;
	}

	@Override
	public CardName containingCard() {
		return container.containingCard();
	}

	@Override
	public String writeToXML(XMLElement xe) {
		XMLElement ty = xe.addElement("SolidName");
		ty.setAttribute("name", name);
		if (container == null)
			return null;
		return container.writeToXML(ty);
	}

	public int compareTo(SolidName other) {
		int cs = 0;
		if (container != null && other.container == null)
			return -1;
		else if (container == null && other.container != null)
			return 1;
		else if (container != null && other.container != null)
			cs = container.compareTo(other.container);
		if (cs != 0)
			return cs;
		return name.compareTo(other.name);
	}

	@Override
	public <T extends NameOfThing> int compareTo(T other) {
		if (!(other instanceof SolidName))
			return other.getClass().getName().compareTo(this.getClass().getName());
		return this.compareTo((SolidName)other);
	}

}
