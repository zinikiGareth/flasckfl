package org.flasck.flas.parsedForm;

import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.commonBase.Locatable;

public class TemplateEvent implements Locatable {
	public final InputPosition location;
	public final String handler;
	public final Template source;
	private ObjectMethod eventHandler;

	public TemplateEvent(InputPosition location, String handler, Template source) {
		this.location = location;
		this.handler = handler;
		this.source = source;
	}

	@Override
	public InputPosition location() {
		return location;
	}

	public void bindHandler(ObjectMethod om) {
		this.eventHandler = om;
		om.eventSource(source);
	}
	
	public ObjectMethod eventHandler() {
		return this.eventHandler;
	}
}
