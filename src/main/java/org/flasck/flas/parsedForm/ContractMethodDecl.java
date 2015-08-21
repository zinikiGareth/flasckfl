package org.flasck.flas.parsedForm;

import java.io.Serializable;
import java.util.List;

@SuppressWarnings("serial")
public class ContractMethodDecl implements Comparable<ContractMethodDecl>, Serializable {
	public final String dir;
	public final String name;
	public final List<Object> args;

	public ContractMethodDecl(String dir, String name, List<Object> args) {
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
}
