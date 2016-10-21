package org.flasck.flas.rewrittenForm;

public class EventHandlerInContext {
	public final String name;
	public final RWEventHandlerDefinition handler;

	public EventHandlerInContext(String name, RWEventHandlerDefinition handler) {
		this.name = name;
		this.handler = handler;
	}
}
