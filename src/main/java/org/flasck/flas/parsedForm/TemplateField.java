package org.flasck.flas.parsedForm;

import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.commonBase.Locatable;
import org.ziniki.splitter.FieldType;
import org.zinutils.exceptions.NotImplementedException;

public class TemplateField implements Locatable {
	private final InputPosition location;
	public final String text;
	private FieldType type;

	public TemplateField(InputPosition location, String text) {
		this.location = location;
		this.text = text;
	}

	@Override
	public InputPosition location() {
		return location;
	}

	public FieldType type() {
		if (type == null) {
			throw new NotImplementedException("type has not been bound");
		}
		return type;
	}

	public void fieldType(FieldType fieldType) {
		this.type = fieldType;
	}
}
