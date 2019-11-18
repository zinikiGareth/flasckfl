package org.flasck.flas.parsedForm;

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.commonBase.Locatable;
import org.flasck.flas.commonBase.names.SolidName;
import org.flasck.flas.parser.ContractMethodConsumer;
import org.flasck.flas.repository.RepositoryEntry;
import org.flasck.flas.tc3.NamedType;
import org.flasck.flas.tc3.Type;
import org.zinutils.exceptions.NotImplementedException;

public class ContractDecl implements Locatable, ContractMethodConsumer, RepositoryEntry, NamedType {
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
	public String signature() {
		return contractName.uniqueName();
	}

	@Override
	public int argCount() {
		throw new NotImplementedException();
	}

	@Override
	public Type get(int pos) {
		throw new NotImplementedException();
	}

	@Override
	public boolean incorporates(InputPosition pos, Type other) {
		throw new NotImplementedException();
	}

	@Override
	public String toString() {
		return "Contract[" + contractName.uniqueName() + "]";
	}

	@Override
	public void dumpTo(PrintWriter pw) {
		pw.println(toString());
	}

	public ContractMethodDecl getMethod(String mname) {
		for (ContractMethodDecl m : methods) {
			if (m.name.name.equals(mname))
				return m;
		}
		return null;
	}
}
