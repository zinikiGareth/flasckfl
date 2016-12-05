package org.flasck.flas.rewrittenForm;

import org.zinutils.exceptions.UtilException;

public class EventHandlerInContext {
	public final String cardName;
	public final String name;
	public final RWEventHandlerDefinition handler;

	public EventHandlerInContext(String cardName, String name, RWEventHandlerDefinition handler) {
		this.cardName = cardName;
		if (this.cardName == null)
			throw new UtilException("Must have a card name for methods");
		this.name = name;
		this.handler = handler;
	}
}
