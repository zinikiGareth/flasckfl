package org.flasck.flas.parsedForm;

import java.util.List;

import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.commonBase.Locatable;
import org.ziniki.splitter.FieldType;

public class TargetZone implements Locatable {
	public final InputPosition location;
	@Deprecated
	public final String text;
	private FieldType type;
	private final List<Object> fields;

	public TargetZone(InputPosition location, String text, List<Object> fields) {
		this.location = location;
		this.text = text;
		this.fields = fields;
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
	
	public int length() {
		return fields.size();
	}
	
	public Object label(int f) {
		return fields.get(f);
	}

	@Override
	public String toString() {
		return "Zone[" + text + "]";
	}
}
