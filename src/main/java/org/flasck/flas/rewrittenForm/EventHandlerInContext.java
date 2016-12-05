package org.flasck.flas.rewrittenForm;

public class EventHandlerInContext {
	public final String cardName;
	public final String name;
	public final RWEventHandlerDefinition handler;

	public EventHandlerInContext(String cardName, String name, RWEventHandlerDefinition handler) {
		this.cardName = cardName;
		this.name = name;
		this.handler = handler;
	}
}
