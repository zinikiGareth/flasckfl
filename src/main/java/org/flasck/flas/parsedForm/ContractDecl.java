package org.flasck.flas.parsedForm;

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.commonBase.Locatable;
import org.flasck.flas.commonBase.names.SolidName;
import org.flasck.flas.parser.ContractMethodConsumer;
import org.flasck.flas.repository.RepositoryEntry;

public class ContractDecl implements Locatable, ContractMethodConsumer, RepositoryEntry {
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
	
	public SolidName name() {
		return contractName;
	}

	public void addMethod(ContractMethodDecl md) {
		methods.add(md);
	}
	
	@Override
	public String toString() {
		return "Contract[" + contractName.uniqueName() + "]";
	}

	@Override
	public void dumpTo(PrintWriter pw) {
		pw.println(toString());
	}
}
