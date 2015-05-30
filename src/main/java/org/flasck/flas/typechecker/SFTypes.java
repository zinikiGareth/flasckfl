package org.flasck.flas.typechecker;

import java.util.ArrayList;
import java.util.List;

import org.flasck.flas.vcode.hsieForm.Var;

public class SFTypes {
	public class Entry {
		final Var var;
		final String field;
		final Object type;
		
		public Entry(Var var, String field, Object type) {
			this.var = var;
			this.field = field;
			this.type = type;
		}
	}

	private final SFTypes inside;
	private final List<Entry> entries = new ArrayList<Entry>();

	public SFTypes(SFTypes inside) {
		this.inside = inside;
	}

	public void put(Var var, String field, Object fr) {
		entries.add(new Entry(var, field, fr));
	}

	public Object get(Var from, String field) {
		for (Entry e : entries) {
			if (from.equals(e.var) && field.equals(e.field))
				return e.type;
		}
		if (inside != null)
			return inside.get(from, field);
		return null;
	}

}
