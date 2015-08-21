package org.flasck.flas.parsedForm;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import org.flasck.flas.blockForm.InputPosition;

@SuppressWarnings("serial")
public class ContractDecl implements Serializable {
	public final transient InputPosition location;
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
