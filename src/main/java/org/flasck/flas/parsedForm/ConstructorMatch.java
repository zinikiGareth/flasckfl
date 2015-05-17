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
	
	@Override
	public String toString() {
		StringBuilder ret = new StringBuilder(ctor);
		if (!args.isEmpty()) {
			ret.append(" { ");
			for (Field f : args) {
				ret.append(f.field + ": " + f.patt + " ");
			}
			ret.append("}");
		}
		return ret.toString();
	}
}
