package org.flasck.flas.parsedForm;

import java.util.ArrayList;
import java.util.List;

import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.commonBase.Locatable;

public class ConstructorMatch implements Locatable {
	public class Field implements Locatable {
		public final String field;
		public final Object patt;
		private InputPosition loc;
		
		public Field(InputPosition loc, String field, Object patt) {
			this.loc = loc;
			this.field = field;
			this.patt = patt;
		}
		
		
		@Override
		public InputPosition location() {
			return loc;
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
	public InputPosition location() {
		return location;
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
