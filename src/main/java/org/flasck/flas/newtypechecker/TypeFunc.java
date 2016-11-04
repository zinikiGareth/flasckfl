package org.flasck.flas.newtypechecker;

import java.util.ArrayList;
import java.util.List;

import org.flasck.flas.vcode.hsieForm.Var;

public class TypeFunc extends TypeInfo {
	public final List<TypeInfo> args = new ArrayList<TypeInfo>();
	
	public TypeFunc(List<Var> vars, int nformal, TypeVar returns) {
		for (int i=0;i<nformal;i++)
			this.args.add(new TypeVar(vars.get(i)));
		this.args.add(returns);
	}

	public TypeFunc(List<TypeInfo> args, TypeInfo returns) {
		for (TypeInfo ti : args)
			this.args.add(ti);
		this.args.add(returns);
	}

	public TypeFunc(List<TypeInfo> args) {
		this.args.addAll(args);
	}

	@Override
	public String toString() {
		String sep = "";
		StringBuilder ret = new StringBuilder();
		for (TypeInfo a : args) {
			ret.append(sep);
			sep = "->";
			ret.append(a);
		}
		return ret.toString();
	}
}
