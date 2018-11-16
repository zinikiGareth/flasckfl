package org.flasck.flas.parsedForm;

import java.util.ArrayList;
import java.util.List;

import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.commonBase.Locatable;
import org.flasck.flas.commonBase.names.SolidName;
import org.flasck.flas.parser.ContractMethodConsumer;

public class ContractDecl implements Locatable, ContractMethodConsumer {
	public final List<ContractMethodDecl> methods = new ArrayList<ContractMethodDecl>();
	public final transient boolean generate;
	public final InputPosition kw;
	private final InputPosition loc;
	private SolidName contractName;

	public ContractDecl(InputPosition kw, InputPosition location, SolidName structName) {
		this.kw = kw;
		this.loc = location;
		this.contractName = structName;
		this.generate = true;
	}

	@Override
	public InputPosition location() {
		return loc;
	}
	
	public SolidName nameAsName() {
		return contractName;
	}

	public void addMethod(ContractMethodDecl md) {
		methods.add(md);
	}
	
	@Override
	public String toString() {
		return "contract " + contractName.uniqueName();
	}
}
