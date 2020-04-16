package org.flasck.flas.parsedForm;

import java.util.List;

import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.commonBase.Pattern;
import org.flasck.flas.commonBase.names.FunctionName;
import org.flasck.flas.tc3.Type;
import org.zinutils.exceptions.NotImplementedException;

public class ObjectCtor extends ObjectActionHandler implements WithTypeSignature {
	private Type od;

	public ObjectCtor(InputPosition location, Type od, FunctionName name, List<Pattern> args) {
		super(location, name, args);
		this.od = od;
	}

	public int argCountIncludingContracts() {
		return argCount() + ((ObjectDefn)od).contracts.size();
	}
	
	@Override
	public ContractMethodDecl contractMethod() {
		return null;
	}

	@Override
	public String signature() {
		return od.signature();
	}

	@Override
	public boolean hasObject() {
		return true;
	}

	@Override
	public ObjectDefn getObject() {
		return (ObjectDefn) od;
	}

	@Override
	public boolean hasImplements() {
		return false;
	}

	@Override
	public Implements getImplements() {
		throw new NotImplementedException();
	}

	@Override
	public String toString() {
		return "ctor " + name().uniqueName();
	}
}
