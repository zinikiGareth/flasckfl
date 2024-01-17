package org.flasck.flas.parsedForm;

import java.util.List;

import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.commonBase.Locatable;
import org.ziniki.splitter.FieldType;

public class TargetZone implements Locatable {
	public static class Qualifier implements Locatable {
		public final InputPosition location;
		public final String qualifyingTemplate;

		public Qualifier(InputPosition location, String qualifyingTemplate) {
			this.location = location;
			this.qualifyingTemplate = qualifyingTemplate;
		}

		@Override
		public InputPosition location() {
			return location;
		}
	}

	public final InputPosition location;
	private List<FieldType> types;
	public final List<Object> fields;

	public TargetZone(InputPosition location, List<Object> fields) {
		this.location = location;
		this.fields = fields;
	}

	@Override
	public InputPosition location() {
		return location;
	}

	public boolean isWholeCard() {
		return fields.isEmpty();
	}

	public void bindTypes(List<FieldType> fieldTypes) {
		this.types = fieldTypes;
	}

	public List<FieldType> types() {
		return types;
	}
	
	public int length() {
		return fields.size();
	}
	
	public Object label(int f) {
		return fields.get(f);
	}

	@Override
	public String toString() {
		String[] fs = new String[fields.size()];
		for (int i=0;i<fields.size();i++) {
			Object o = fields.get(i);
			if (o instanceof String)
				fs[i] = (String) o;
			else
				fs[i] = Integer.toString((Integer)o);
		}
		return String.join(".", fs);
	}
}
