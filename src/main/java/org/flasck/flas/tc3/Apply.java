package org.flasck.flas.tc3;

import java.util.ArrayList;
import java.util.List;

public class Apply implements Type {
	public final List<Type> tys;

	public Apply(Type... types) {
		if (types.length < 2)
			throw new RuntimeException("Must have at least one input and one output");
		tys = new ArrayList<>();
		for (Type t : types)
			tys.add(t);
	}

	public Apply(List<Type> types) {
		if (types.size() < 2)
			throw new RuntimeException("Must have at least one input and one output");
		this.tys = types;
	}

	public Apply(List<Type> argTypes, Type result) {
		if (argTypes.isEmpty())
			throw new RuntimeException("Must have at least one input and one output");
		tys = new ArrayList<>();
		tys.addAll(argTypes);
		tys.add(result);
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

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		for (Type t : tys) {
			sb.append("-->");
			if (t == null)
				sb.append("<<UNDEFINED>>");
			else if (t instanceof TypeConstraintSet && !((TypeConstraintSet)t).isResolved())
				sb.append("UnifiableType");
			else
				sb.append("(" + t.toString() + ")");
		}
		return sb.toString();
	}
}
