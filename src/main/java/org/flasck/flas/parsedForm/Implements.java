package org.flasck.flas.parsedForm;

import java.util.ArrayList;
import java.util.List;

import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.commonBase.Locatable;
import org.flasck.flas.commonBase.names.NameOfThing;
import org.flasck.flas.parser.ImplementationMethodConsumer;

public class Implements implements Locatable, ImplementationMethodConsumer {
	public final InputPosition kw;
	private InputPosition location;
	private TypeReference implementing;
	private final NameOfThing myName;
	private ContractDecl actualType;
	public final List<ObjectMethod> implementationMethods = new ArrayList<>();

	public Implements(InputPosition kw, InputPosition location, TypeReference implementing, NameOfThing myName) {
		this.kw = kw;
		this.location = location;
		this.implementing = implementing;
		this.myName = myName;
	}

	@Override
	public InputPosition location() {
		return location;
	}
	
	public TypeReference implementsType() {
		return implementing;
	}

	public NameOfThing name() {
		return myName;
	}

	@Override
	public void addImplementationMethod(ObjectMethod method) {
		implementationMethods.add(method);
	}

	public ContractDecl actualType() {
		return actualType;
	}

	public void bindActualType(ContractDecl actualType) {
		this.actualType = actualType;
	}

	@Override
	public String toString() {
		return myName.uniqueName();
	}
}
