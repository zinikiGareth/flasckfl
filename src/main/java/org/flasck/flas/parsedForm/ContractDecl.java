package org.flasck.flas.parsedForm;

import java.util.ArrayList;
import java.util.List;

import org.flasck.flas.blockForm.InputPosition;

public class ContractDecl {
	public final InputPosition location;
	public final String contractName;
	public final List<ContractMethodDecl> methods = new ArrayList<ContractMethodDecl>();

	public ContractDecl(InputPosition location, String contractName) {
		this.location = location;
		this.contractName = contractName;
	}

	public void addMethod(ContractMethodDecl md) {
		methods.add(md);
	}

}
