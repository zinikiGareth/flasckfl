package org.flasck.flas.typechecker;

import java.util.ArrayList;
import java.util.List;

public class TypeExpr {
	public final String type;
	public final List<TypeExpr> args;

	public TypeExpr(String type, List<TypeExpr> args) {
		this.type = type;
		if (args == null)
			this.args = new ArrayList<TypeExpr>();
		else
			this.args = args;
	}
}
