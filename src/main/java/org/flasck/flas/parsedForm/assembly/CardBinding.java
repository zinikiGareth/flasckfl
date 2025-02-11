package org.flasck.flas.parsedForm.assembly;

import java.io.PrintWriter;

import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.commonBase.names.NameOfThing;
import org.flasck.flas.commonBase.names.VarName;
import org.flasck.flas.parsedForm.StateHolder;
import org.flasck.flas.parsedForm.TypeReference;
import org.flasck.flas.repository.RepositoryEntry;

public class CardBinding implements RepositoryEntry {
	public final TypeReference cardType;
	private final VarName myname;

	public CardBinding(NameOfThing name, VarName var, TypeReference cardType) {
		this.cardType = cardType;
		this.myname = var;
	}

	@Override
	public VarName name() {
		return myname;
	}

	@Override
	public InputPosition location() {
		return myname.location();
	}

	@Override
	public void dumpTo(PrintWriter pw) {
		pw.println("card binding " + myname.uniqueName());
	}

	public StateHolder type() {
		return (StateHolder) cardType.namedDefn();
	}
}