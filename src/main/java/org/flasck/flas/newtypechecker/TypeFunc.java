package org.flasck.flas.newtypechecker;

import java.util.ArrayList;
import java.util.List;

import org.flasck.flas.rewrittenForm.RWStructField;
import org.flasck.flas.vcode.hsieForm.Var;

public class TypeFunc extends TypeInfo {
	public final List<TypeInfo> args = new ArrayList<TypeInfo>();
	
	public TypeFunc(List<RWStructField> args, String returns) {
		for (RWStructField sf : args)
			this.args.add(new NamedType(sf.type.name())); // TODO: this is not quite good enough; need some kind of Type->TypeInfo convertor really (recursive!)
		this.args.add(new NamedType(returns));
	}

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
