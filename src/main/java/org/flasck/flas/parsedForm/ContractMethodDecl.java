package org.flasck.flas.parsedForm;

import java.util.List;

public class ContractMethodDecl {
	public final String dir;
	public final String name;
	public final List<Object> args;

	public ContractMethodDecl(String dir, String name, List<Object> args) {
		this.dir = dir;
		this.name = name;
		this.args = args;
	}

}
