package org.flasck.flas.newtypechecker;

import java.util.ArrayList;
import java.util.List;

import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.vcode.hsieForm.Var;

public class TypeFunc extends TypeInfo {
	private final InputPosition location;
	public final List<TypeInfo> args = new ArrayList<TypeInfo>();
	
	public TypeFunc(InputPosition location, List<Var> vars, int nformal, TypeVar returns) {
		this.location = location;
		for (int i=0;i<nformal;i++)
			this.args.add(new TypeVar(location, vars.get(i)));
		this.args.add(returns);
	}

	public TypeFunc(InputPosition location, List<TypeInfo> args, TypeInfo returns) {
		this.location = location;
		for (TypeInfo ti : args)
			this.args.add(ti);
		this.args.add(returns);
	}

	public TypeFunc(InputPosition location, List<TypeInfo> args) {
		this.location = location;
		this.args.addAll(args);
	}
	
	public InputPosition location() {
		return location;
	}

	@Override
	public String toString() {
		String sep = "";
		if (args.size() == 1)
			sep = "->";
		StringBuilder ret = new StringBuilder();
		for (TypeInfo a : args) {
			ret.append(sep);
			sep = "->";
			if (a instanceof TypeFunc) {
				ret.append("(");
				ret.append(a);
				ret.append(")");
			} else
				ret.append(a);
		}
		return ret.toString();
	}
}
