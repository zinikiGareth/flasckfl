package org.flasck.flas.parsedForm;

import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.commonBase.names.SolidName;
import org.flasck.flas.tc3.NamedType;
import org.flasck.flas.tc3.Type;
import org.zinutils.exceptions.NotImplementedException;

public class ContractDeclDir implements NamedType {
	public final ContractDecl decl;
	public final String dir;

	public ContractDeclDir(ContractDecl defn, String dir) {
		this.decl = defn;
		this.dir = dir;
	}

	@Override
	public SolidName name() {
		return new SolidName(decl.name(), dir);
	}

	@Override
	public String signature() {
		return name().uniqueName();
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
		return signature().equals(other.signature());
	}
}