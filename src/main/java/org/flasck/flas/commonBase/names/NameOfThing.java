package org.flasck.flas.commonBase.names;

public interface NameOfThing {
	String baseName();
	String uniqueName();
	
	String jsName();
	String javaName();
	String javaPackageName();
	String javaClassName();

	NameOfThing container();
	NameOfThing containingCard();

	<T extends NameOfThing> int compareTo(T other);

	PackageName packageName();
}
