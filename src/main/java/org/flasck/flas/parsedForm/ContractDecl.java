package org.flasck.flas.parsedForm;

import java.util.ArrayList;
import java.util.List;

import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.commonBase.Locatable;

public class ContractDecl implements Locatable {
	public final List<ContractMethodDecl> methods = new ArrayList<ContractMethodDecl>();
	public final transient boolean generate;
	public final InputPosition kw;
	private final InputPosition loc;
	private String contractName;

	public ContractDecl(InputPosition kw, InputPosition location, String contractName) {
		this.kw = kw;
		this.loc = location;
		this.contractName = contractName;
		this.generate = true;
	}

	@Override
	public InputPosition location() {
		return loc;
	}
	
	public String name() {
		return contractName;
	}

	public void addMethod(ContractMethodDecl md) {
		methods.add(md);
	}
	
	@Override
	public String toString() {
		return "contract " + name();
	}
}
