package org.flasck.flas.parsedForm;

import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.commonBase.Locatable;

public class TemplateField implements Locatable {
	private final InputPosition location;
	public final String text;

	public TemplateField(InputPosition location, String text) {
		this.location = location;
		this.text = text;
	}

	@Override
	public InputPosition location() {
		return location;
	}

}
