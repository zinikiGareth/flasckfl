package org.flasck.flas.commonBase.names;

import org.zinutils.xml.XMLElement;

public interface NameOfThing {
	String uniqueName();
	
	String jsName();
	String jsUName();
	String javaName();
	String javaPackageName();
	String javaClassName();

	NameOfThing containingCard();

	<T extends NameOfThing> int compareTo(T other);

	String writeToXML(XMLElement xe);

}
