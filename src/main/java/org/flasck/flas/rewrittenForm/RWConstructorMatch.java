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

	public RWConstructorMatch(InputPosition loc, PackageVar pv) {
		if (loc == null)
			System.out.println("null position cm2");
		this.location = loc;
		this.ctor = null;
		this.ref = (ExternalRef) pv.defn;
	}
	
	@Override
	public String toString() {
		StringBuilder ret = new StringBuilder(ref.uniqueName());
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
