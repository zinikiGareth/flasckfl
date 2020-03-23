package org.flasck.flas.rewrittenForm;

import java.util.List;

import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.commonBase.AsString;
import org.flasck.flas.commonBase.Locatable;
import org.flasck.flas.commonBase.Pattern;
import org.flasck.flas.commonBase.names.FunctionName;
import org.flasck.flas.types.FunctionType;

public class RWContractMethodDecl implements Locatable, Comparable<RWContractMethodDecl> {
	private final InputPosition pos;
	public final boolean required;
	public final String name;
	public final List<Pattern> args;
	public final FunctionType type;
	public final RWTypedPattern handler;

	public RWContractMethodDecl(InputPosition pos, boolean required, FunctionName name, List<Pattern> args, FunctionType type, RWTypedPattern handler) {
		this.pos = pos;
		this.required = required;
		this.name = name.name;
		this.args = args;
		this.type = type;
		this.handler = handler;
	}

	@Override
	public int compareTo(RWContractMethodDecl o) {
		return name.compareTo(o.name);
	}
	
	public FunctionType getType() {
		return type;
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder(name);
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
