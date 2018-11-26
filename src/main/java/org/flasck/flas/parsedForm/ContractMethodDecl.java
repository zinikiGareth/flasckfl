package org.flasck.flas.parsedForm;

import java.util.List;

import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.commonBase.AsString;
import org.flasck.flas.commonBase.Locatable;
import org.flasck.flas.commonBase.names.FunctionName;

public class ContractMethodDecl implements Locatable, Comparable<ContractMethodDecl> {
	public final InputPosition rkw;
	public final InputPosition dkw;
	private final InputPosition pos;
	public final boolean required;
	public final ContractMethodDir dir;
	public final FunctionName name;
	public final List<Object> args;

	public ContractMethodDecl(InputPosition rkw, InputPosition dkw, InputPosition pos, boolean required, ContractMethodDir dir, FunctionName name, List<Object> args) {
		this.rkw = rkw;
		this.dkw = dkw;
		this.pos = pos;
		this.required = required;
		this.dir = dir;
		this.name = name;
		this.args = args;
	}

	@Override
	public int compareTo(ContractMethodDecl o) {
		int dc = dir.compareTo(o.dir);
		if (dc != 0) return dc;
		return name.compareTo(o.name);
	}
	
	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder(required?"required":"optional");
		sb.append(" ");
		sb.append(dir);
		sb.append(" ");
		sb.append(name.name);
		for (Object o : args) {
			sb.append(" ");
			sb.append(o.toString());
		}
		return sb.toString();
	}

	@Override
	public InputPosition location() {
		return pos;
	}
}
