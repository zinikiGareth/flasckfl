package org.flasck.flas.parsedForm;

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.commonBase.Locatable;
import org.flasck.flas.commonBase.names.FunctionName;
import org.flasck.flas.commonBase.names.SolidName;
import org.flasck.flas.commonBase.names.VarName;
import org.flasck.flas.parser.ContractMethodConsumer;
import org.flasck.flas.repository.LoadBuiltins;
import org.flasck.flas.repository.RepositoryEntry;
import org.flasck.flas.tc3.NamedType;
import org.flasck.flas.tc3.Type;
import org.flasck.flas.tc3.UnifiableType;
import org.zinutils.exceptions.NotImplementedException;

public class ContractDecl implements Locatable, ContractMethodConsumer, RepositoryEntry, NamedType {
	public enum ContractType { CONTRACT, SERVICE, HANDLER }; 
	public final List<ContractMethodDecl> methods = new ArrayList<ContractMethodDecl>();
	public final transient boolean generate;
	public final ContractType type;
	public final InputPosition kw;
	private final InputPosition loc;
	private final SolidName contractName;

	public ContractDecl(InputPosition kw, InputPosition location, ContractType type, SolidName ctrName) {
		this(kw, location, type, ctrName, true);
	}

	public ContractDecl(InputPosition kw, InputPosition location, ContractType type, SolidName ctrName, boolean generate) {
		this.kw = kw;
		this.loc = location;
		this.type = type;
		this.contractName = ctrName;
		this.generate = generate;
		if (type == ContractType.HANDLER) {
			addMethod(new ContractMethodDecl(kw, kw, location, false, FunctionName.contractMethod(location, ctrName, "success"), new ArrayList<TypedPattern>(), null));
			addMethod(new ContractMethodDecl(kw, kw, location, false, FunctionName.contractMethod(location, ctrName, "failure"), Arrays.asList(new TypedPattern(location, LoadBuiltins.stringTR, new VarName(location, ctrName, "msg"))), null));
		}
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
		if (other == this)
			return true;
		else if (other instanceof HandlerImplements) {
			HandlerImplements hi = (HandlerImplements) other;
			if (hi.implementsType().namedDefn() == this)
				return true;
		} else if (other instanceof UnifiableType) {
			UnifiableType ut = (UnifiableType)other;
			ut.canBeType(pos, this);
			return true;
		}
		return false;
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
