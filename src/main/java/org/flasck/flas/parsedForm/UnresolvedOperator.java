package org.flasck.flas.parsedForm;

import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.commonBase.Expr;
import org.flasck.flas.commonBase.names.FunctionName;
import org.flasck.flas.commonBase.names.NameOfThing;
import org.flasck.flas.repository.RepositoryEntry;
import org.flasck.flas.tc3.Type;
import org.zinutils.exceptions.NotImplementedException;

public class UnresolvedOperator implements Expr, WithTypeSignature {
	public final InputPosition location;
	public final String op;
	private RepositoryEntry definition;

	public UnresolvedOperator(InputPosition location, String op) {
		this.location = location;
		this.op = op;
	}
	
	@Override
	public InputPosition location() {
		return location;
	}

	@Override
	public String toString() {
		return op;
	}

	public void bind(RepositoryEntry entry) {
		this.definition = entry;
	}
	
	@Override
	public Type type() {
		if (definition instanceof Type)
			return (Type) definition;
		else if (definition instanceof FunctionDefinition)
			return ((FunctionDefinition) definition).type();
		else
			throw new NotImplementedException();
	}

	public RepositoryEntry defn() {
		return this.definition;
	}

	@Override
	public NameOfThing name() {
		return FunctionName.function(location, null, op);
	}

	@Override
	public String signature() {
		return type().signature();
	}

	@Override
	public int argCount() {
		return type().argCount();
	}
}
