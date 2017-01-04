package org.flasck.flas.rewrittenForm;

import java.util.List;

import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.commonBase.AsString;
import org.flasck.flas.commonBase.Locatable;
import org.flasck.flas.commonBase.names.FunctionName;
import org.flasck.flas.types.FunctionType;

public class RWContractMethodDecl implements Locatable, Comparable<RWContractMethodDecl> {
	private final InputPosition pos;
	public final boolean required;
	public final String dir;
	public final String name;
	public final List<Object> args;
	public final FunctionType type;

	public RWContractMethodDecl(InputPosition pos, boolean required, String dir, FunctionName name, List<Object> args, FunctionType type) {
		this.pos = pos;
		this.required = required;
		this.dir = dir;
		this.name = name.name;
		this.args = args;
		this.type = type;
	}

	@Override
	public int compareTo(RWContractMethodDecl o) {
		int dc = dir.compareTo(o.dir);
		if (dc != 0) return dc;
		return name.compareTo(o.name);
	}
	
	public FunctionType getType() {
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

	@Override
	public InputPosition location() {
		return pos;
	}
}
