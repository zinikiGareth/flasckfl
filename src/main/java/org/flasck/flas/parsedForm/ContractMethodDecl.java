package org.flasck.flas.parsedForm;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.typechecker.Type;
import org.zinutils.exceptions.UtilException;

@SuppressWarnings("serial")
public class ContractMethodDecl implements Comparable<ContractMethodDecl>, Serializable {
	public final boolean required;
	public final String dir;
	public final String name;
	public final List<Object> args;
	public final Type type;

	public ContractMethodDecl(InputPosition pos, boolean required, String dir, String name, List<Object> args) {
		this.required = required;
		this.dir = dir;
		this.name = name;
		this.args = args;
		List<Type> types = new ArrayList<Type>();
		for (Object o : args) {
			if (o instanceof TypedPattern) {
				// It seems to me that TypedPattern should already have resolved this to a type for us ...
				TypedPattern tp = (TypedPattern)o;
				types.add(Type.reference(tp.typeLocation, tp.type));
			} else if (o instanceof VarPattern) {
				types.add(Type.reference(null, "Any"));
			} else
				throw new UtilException("Cannot handle type " + o.getClass());
		}
		types.add(Type.reference(pos, "Send"));
		this.type = Type.function(pos, types);
	}

	public ContractMethodDecl(boolean required, String dir, String name, List<Object> args, Type type) {
		this.required = required;
		this.dir = dir;
		this.name = name;
		this.args = args;
		this.type = type;
	}

	@Override
	public int compareTo(ContractMethodDecl o) {
		int dc = dir.compareTo(o.dir);
		if (dc != 0) return dc;
		return name.compareTo(o.name);
	}
	
	public Type getType() {
		return type;
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder(dir + " " + name);
		for (Object o : args) {
			sb.append(" ");
			sb.append(((AsString)o).asString());
		}
		return sb.toString();
	}
}
