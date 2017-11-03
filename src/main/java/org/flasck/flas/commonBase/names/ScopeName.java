package org.flasck.flas.commonBase.names;

import org.zinutils.exceptions.NotImplementedException;
import org.zinutils.xml.XMLElement;

public class ScopeName implements NameOfThing, Comparable<ScopeName> {
	private final NameOfThing inside;
	private final String myname;

	public ScopeName(NameOfThing inside, String myname) {
		this.inside = inside;
		this.myname = myname;
	}

	@Override
	public CardName containingCard() {
		return inside.containingCard();
	}

	public String uniqueName() {
		if (inside == null)
			return myname;
		else
			return inside.uniqueName() + "." + myname;
	}
	
	@Override
	public String jsName() {
		return (inside == null?myname:inside.jsName()+"."+myname);
	}

	@Override
	public String jsUName() {
		return inside.jsName() + "._" + myname;
	}
	
	@Override
	public String javaName() {
		throw new NotImplementedException();
	}

	@Override
	public String javaClassName() {
		return inside.javaClassName();
	}

	public String javaDefiningClassName() {
		if (inside instanceof HandlerName)
			return inside.javaClassName();
		else
			return inside.javaClassName() + "." + myname;
	}
	
	public int compareTo(ScopeName other) {
		int cs = 0;
		if (inside != null && other.inside == null)
			return -1;
		else if (inside == null && other.inside != null)
			return 1;
		else if (inside != null && other.inside != null)
			cs = inside.compareTo(other.inside);
		if (cs != 0)
			return cs;
		if (myname == null && other.myname == null)
			return 0;
		else if (myname == null)
			return -1;
		else
			return myname.compareTo(other.myname);
	}

	@Override
	public <T extends NameOfThing> int compareTo(T other) {
		if (!(other instanceof ScopeName))
			return other.getClass().getName().compareTo(this.getClass().getName());
		return this.compareTo((ScopeName)other);
	}

	@Override
	public String writeToXML(XMLElement xe) {
		// TODO Auto-generated method stub
		return null;
	}

}
