package org.flasck.flas.rewrittenForm;

import java.util.ArrayList;
import java.util.List;

import org.flasck.flas.blockForm.InputPosition;

public class RWConstructorMatch {
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
	public final ExternalRef ref;
	public final List<Field> args = new ArrayList<Field>();
	public final InputPosition location;

	public RWConstructorMatch(InputPosition loc, String ctor) {
		if (loc == null)
			System.out.println("null position cm1");
		this.location = loc;
		this.ctor = ctor;
		this.ref = null;
	}
	
	public RWConstructorMatch(InputPosition loc, ExternalRef ref) {
		if (loc == null)
			System.out.println("null position cm2");
		this.location = loc;
		this.ctor = null;
		this.ref = ref;
	}
	
	@Override
	public String toString() {
		StringBuilder ret = new StringBuilder(ctor != null?ctor:ref.uniqueName());
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
