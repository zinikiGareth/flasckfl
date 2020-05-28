package org.flasck.flas.commonBase.names;

import org.zinutils.exceptions.NotImplementedException;
import org.zinutils.xml.XMLElement;

public class HandlerName implements NameOfThing, Comparable<HandlerName> {
	public final NameOfThing name;
	public final String baseName;

	public HandlerName(NameOfThing n, String baseName) {
		this.name = n;
		this.baseName = baseName;
	}
	
	@Override
	public NameOfThing container() {
		return name;
	}

	@Override
	public String baseName() {
		return baseName;
	}
	
	@Override
	public NameOfThing containingCard() {
		return name.containingCard();
	}

	public String uniqueName() {
		if (name == null || name.uniqueName() == null || name.uniqueName().length() == 0)
			return baseName;
		else
			return name.uniqueName() + "." + baseName;
	}
	
	@Override
	public String javaName() {
		throw new NotImplementedException();
	}

	@Override
	public String javaClassName() {
		return name.uniqueName() + "$" + baseName;
	}

	@SuppressWarnings("unchecked")
	@Override
	public int compareTo(HandlerName o) {
		int cc = 0;
		if (name != null && o.name == null)
			return -1;
		else if (name == null && o.name != null)
			return 1;
		else if (name != null && o.name != null)
			cc = ((Comparable<NameOfThing>)name).compareTo(o.name);
		if (cc != 0)
			return cc;
		return baseName.compareTo(o.baseName);
	}

	@Override
	public <T extends NameOfThing> int compareTo(T other) {
		if (!(other instanceof HandlerName))
			return other.getClass().getName().compareTo(this.getClass().getName());
		return this.compareTo((HandlerName)other);
	}

	@Override
	public String jsName() {
		if (name == null)
			return baseName;
		return name.jsName() + "." + baseName;
	}

	@Override
	public String jsUName() {
		if (name == null)
			return baseName;
		return name.jsName() + "._" + baseName;
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

	public PackageName packageName() {
		NameOfThing ret = name;
		while (ret != null) {
			if (ret instanceof PackageName)
				return (PackageName) ret;
			ret = ret.container();
		}
		throw new RuntimeException("No PackageName found");
	}

}
