package org.flasck.flas.parsedForm;

import java.util.ArrayList;
import java.util.List;

import org.flasck.flas.blockForm.InputPosition;

public class ConstructorMatch {
	public class Field {
		public final String field;
		public final Object patt;
		
		public Field(String field, Object patt) {
			this.field = field;
			this.patt = patt;
		}
		@Override
		public String toString() {
			return "CMF[" + field + "]";
		}
	}

	public final String ctor;
	public final List<Field> args = new ArrayList<Field>();
	public final InputPosition location;

	public ConstructorMatch(InputPosition loc, String ctor) {
		if (loc == null)
			System.out.println("null position cm1");
		this.location = loc;
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
