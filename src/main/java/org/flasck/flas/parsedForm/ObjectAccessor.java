package org.flasck.flas.parsedForm;

import java.io.PrintWriter;

import org.flasck.flas.commonBase.Expr;
import org.flasck.flas.commonBase.names.FunctionName;
import org.flasck.flas.repository.RepositoryEntry;
import org.flasck.flas.tc3.Type;

public class ObjectAccessor implements RepositoryEntry, FieldAccessor {
	private final FunctionDefinition fn;

	public ObjectAccessor(FunctionDefinition fn) {
		this.fn = fn;
	}
	
	public FunctionName name() {
		return fn.name();
	}
	
	@Override
	public Type type() {
		return fn.type();
	}

	public FunctionDefinition function() {
		return fn;
	}

	@Override
	public Expr acor(Expr from) {
		return new MakeAcor(from.location(), fn.name(), from, fn.argCount());
	}

	@Override
	public String toString() {
		return "ObjectAccessor[" + fn + "]";
	}

	@Override
	public void dumpTo(PrintWriter pw) {
		pw.print(toString());
	}
}
