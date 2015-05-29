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

	// Test if two type expressions are exactly the same, to the very comma
	// Type variables would need to be THE SAME variables to pass this test - they are not all created equal
	public boolean identicalTo(TypeExpr add) {
		// TODO Auto-generated method stub
		return false;
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
	
	@Override
	public String toString() {
		return type+(args.isEmpty()?"":"("+args+")");
	}
}
