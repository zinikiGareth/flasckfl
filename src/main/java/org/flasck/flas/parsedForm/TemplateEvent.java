package org.flasck.flas.parsedForm;

import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.commonBase.Locatable;

public class TemplateEvent implements Locatable {
	public final InputPosition location;
	public final String handler;

	public TemplateEvent(InputPosition location, String handler) {
		this.location = location;
		this.handler = handler;
	}

	@Override
	public InputPosition location() {
		return location;
	}
}
