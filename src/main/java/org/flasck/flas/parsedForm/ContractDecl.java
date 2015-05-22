package org.flasck.flas.parsedForm;

import java.util.ArrayList;
import java.util.List;

public class ContractDecl {
	public final String contractName;
	public final List<ContractMethodDecl> methods = new ArrayList<ContractMethodDecl>();

	public ContractDecl(String contractName) {
		this.contractName = contractName;
	}

	public void addMethod(ContractMethodDecl md) {
		methods.add(md);
	}

}
