package org.flasck.flas.parsedForm;

import java.util.List;

import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.commonBase.Pattern;
import org.flasck.flas.commonBase.names.FunctionName;
import org.flasck.flas.tc3.Type;
import org.zinutils.exceptions.NotImplementedException;

public class ObjectCtor extends ObjectActionHandler {
	private Type od;

	public ObjectCtor(InputPosition location, Type od, FunctionName name, List<Pattern> args) {
		super(location, name, args);
		this.od = od;
	}
	
	@Override
	public ContractMethodDecl contractMethod() {
		return null;
	}

	@Override
	public boolean hasObject() {
		return true;
	}

	@Override
	public Type getObject() {
		return od;
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
