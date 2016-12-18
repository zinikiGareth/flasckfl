package org.flasck.flas.commonBase;

import org.flasck.flas.commonBase.names.CardName;

public interface NameOfThing {

	String uniqueName();
	
	String jsName();

	CardName containingCard();

	<T extends NameOfThing> int compareTo(T other);
}
