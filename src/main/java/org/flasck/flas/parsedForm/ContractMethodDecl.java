package org.flasck.flas.parsedForm;

import java.util.List;

import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.commonBase.AsString;
import org.flasck.flas.commonBase.Locatable;

public class ContractMethodDecl implements Locatable, Comparable<ContractMethodDecl> {
	public final InputPosition rkw;
	public final InputPosition dkw;
	private final InputPosition pos;
	public final boolean required;
	public final String dir;
	public final String name;
	public final List<Object> args;

	public ContractMethodDecl(InputPosition rkw, InputPosition dkw, InputPosition pos, boolean required, String dir, String name, List<Object> args) {
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
