package org.flasck.flas.tc3;

import java.util.ArrayList;
import java.util.List;

public class Apply implements Type {
	private List<Type> tys = new ArrayList<>();

	public Apply(Type... types) {
		if (types.length < 2)
			throw new RuntimeException("Must have at least one input and one output");
		for (Type t : types)
			tys.add(t);
	}

	@Override
	public String signature() {
		StringBuilder sb = new StringBuilder();
		String sep = "";
		for (Type t : tys) {
			sb.append(sep);
			sep = "->";
			if (t == null)
				sb.append("<<UNDEFINED>>");
			else
				sb.append(t.signature());
		}
		return sb.toString();
	}

	@Override
	public int argCount() {
		return tys.size()-1;
	}

	@Override
	public Type get(int pos) {
		return tys.get(pos);
	}

	@Override
	public boolean incorporates(Type other) {
		// TODO Auto-generated method stub
		return false;
	}

}
