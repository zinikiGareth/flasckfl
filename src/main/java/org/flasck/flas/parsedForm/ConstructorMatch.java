package org.flasck.flas.parsedForm;

import java.util.ArrayList;
import java.util.List;

public class ConstructorMatch {
	public class Field {
		public final String field;
		public final Object patt;
		
		public Field(String field, Object patt) {
			this.field = field;
			this.patt = patt;
		}
	}

	public final String ctor;
	public final List<Field> args = new ArrayList<Field>();

	public ConstructorMatch(String ctor) {
		this.ctor = ctor;
	}
}
