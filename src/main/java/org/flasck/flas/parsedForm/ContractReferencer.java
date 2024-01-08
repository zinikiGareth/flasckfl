package org.flasck.flas.parsedForm;

import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.commonBase.Locatable;
import org.flasck.flas.commonBase.names.NameOfThing;
import org.flasck.flas.tc3.NamedType;

public class ContractReferencer implements Locatable {
	public final InputPosition kw;
	private InputPosition location;
	protected final NamedType parent;
	private TypeReference implementing;
	private final NameOfThing myName;

	public ContractReferencer(InputPosition kw, InputPosition location, NamedType parent, TypeReference implementing, NameOfThing myName) {
		this.kw = kw;
		this.location = location;
		this.parent = parent;
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

	public NamedType getParent() {
		return parent;
	}

	public NameOfThing name() {
		return myName;
	}

	public ContractDecl actualType() {
		NamedType defn = implementing.namedDefn();
		if (defn == null)
			return null;
		else if (defn instanceof ContractDecl)
			return (ContractDecl) defn;
		else
			return null;
	}

	@Override
	public String toString() {
		return myName.uniqueName();
	}
}
