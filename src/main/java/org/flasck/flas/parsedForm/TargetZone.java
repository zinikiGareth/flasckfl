package org.flasck.flas.parsedForm;

import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.commonBase.Locatable;
import org.ziniki.splitter.FieldType;

public class TargetZone implements Locatable {
	public final InputPosition location;
	public final String text;
	private FieldType type;

	public TargetZone(InputPosition location, String text) {
		this.location = location;
		this.text = text;
	}

	@Override
	public InputPosition location() {
		return location;
	}

	public void bindType(FieldType fieldType) {
		this.type = fieldType;
	}

	public FieldType type() {
		return type;
	}

	@Override
	public String toString() {
		return "Zone[" + text + "]";
	}
}
