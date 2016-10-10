package org.flasck.flas.rewrittenForm;

public class EventHandlerInContext {
	public final Scope scope;
	public final String name;
	public final EventHandlerDefinition handler;

	public EventHandlerInContext(Scope scope, String name, EventHandlerDefinition handler) {
		this.scope = scope;
		this.name = name;
		this.handler = handler;
	}
}
