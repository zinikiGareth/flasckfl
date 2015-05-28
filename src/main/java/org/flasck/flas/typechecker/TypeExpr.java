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
}
