package org.flasck.flas.typechecker;

import java.util.ArrayList;
import java.util.List;

public class TypeExpr {
	public final String type;
	public final List<Object> args;

	public TypeExpr(String type, List<Object> args) {
		this.type = type;
		if (args == null)
			this.args = new ArrayList<Object>();
		else
			this.args = args;
	}

	public TypeExpr(String type, Object... exprs) {
		this.type = type;
		this.args = new ArrayList<Object>();
		for (Object o : exprs)
			args.add(o);
	}
	
	@Override
	public String toString() {
		return "TE["+type+"("+args+")]";
	}

	public boolean containsVar(TypeVar tv) {
		boolean ret = false;
		for (Object o : args) {
			if (o instanceof TypeVar && tv.equals(o))
				return true;
			else if (o instanceof TypeExpr)
				ret |= ((TypeExpr) o).containsVar(tv);
		}
		return ret;
	}
}
