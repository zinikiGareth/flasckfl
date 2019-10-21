package org.flasck.flas.tc3;

import java.util.ArrayList;
import java.util.List;

import org.zinutils.exceptions.NotImplementedException;

public class Application implements Type {
	public final UnifiableType op;
	public final List<Type> tys;

	public Application(UnifiableType op, Type... types) {
		if (types.length < 1)
			throw new RuntimeException("Must have at least one application type");
		this.op = op;
		tys = new ArrayList<>();
		for (Type t : types)
			tys.add(t);
	}

	public Application(UnifiableType op, List<Type> types) {
		this.op = op;
		if (types.size() < 1)
			throw new RuntimeException("Must have at least one application type");
		this.tys = types;
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append("@@(");
		sb.append(op);
		sb.append("@");
		sb.append(tys);
		sb.append(")");
		return sb.toString();
	}

	@Override
	public String signature() {
		throw new NotImplementedException();
	}

	@Override
	public int argCount() {
		throw new NotImplementedException();
	}

	@Override
	public Type get(int pos) {
		throw new NotImplementedException();
	}

	@Override
	public boolean incorporates(Type other) {
		throw new NotImplementedException();
	}
}
