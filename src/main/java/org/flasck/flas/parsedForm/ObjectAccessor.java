package org.flasck.flas.parsedForm;

import java.io.PrintWriter;

import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.commonBase.Expr;
import org.flasck.flas.commonBase.names.FunctionName;
import org.flasck.flas.repository.RepositoryEntry;
import org.flasck.flas.tc3.Type;

public class ObjectAccessor implements RepositoryEntry, FieldAccessor {
	private final StateHolder od;
	private final FunctionDefinition fn;
	public boolean generate = true;

	public ObjectAccessor(StateHolder od, FunctionDefinition fn) {
		this.od = od;
		this.fn = fn;
	}

	public void dontGenerate() {
		this.generate = false;
		fn.generate = false;
	}

	@Override
	public InputPosition location() {
		return fn.location();
	}

	public StateHolder getObject() {
		return od;
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
	public int acorArgCount() {
		return fn.argCount();
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
