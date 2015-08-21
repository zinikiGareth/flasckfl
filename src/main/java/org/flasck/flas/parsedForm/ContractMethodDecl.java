package org.flasck.flas.parsedForm;

import java.io.Serializable;
import java.util.List;

@SuppressWarnings("serial")
public class ContractMethodDecl implements Serializable {
	public final String dir;
	public final String name;
	public final List<Object> args;

	public ContractMethodDecl(String dir, String name, List<Object> args) {
		this.dir = dir;
		this.name = name;
		this.args = args;
	}

}
