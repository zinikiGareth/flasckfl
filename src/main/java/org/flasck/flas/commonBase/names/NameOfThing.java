package org.flasck.flas.commonBase.names;

public interface NameOfThing {

	String uniqueName();
	
	String jsName();

	CardName containingCard();

	<T extends NameOfThing> int compareTo(T other);
}
