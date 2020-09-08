package org.flasck.flas.repository.flim;

import java.util.ArrayList;
import java.util.List;

import org.flasck.flas.commonBase.names.FunctionName;
import org.flasck.flas.errors.ErrorReporter;
import org.flasck.flas.parsedForm.FunctionDefinition;
import org.flasck.flas.parsedForm.StateHolder;
import org.flasck.flas.repository.LoadBuiltins;
import org.flasck.flas.repository.Repository;

public class PendingFunction {
	private final FunctionName fn;
	private final List<PendingArg> args = new ArrayList<>();
	private StateHolder holder;
	private FunctionDefinition fd;

	public PendingFunction(FunctionName fn) {
		this.fn = fn;
	}

	public void create(ErrorReporter errors, Repository repository) {
		System.out.println("resolving " + fn.uniqueName());
		fd = new FunctionDefinition(fn, args.size(), holder);
		fd.dontGenerate();
		repository.functionDefn(errors, fd);
	}
	
	public void bindType(ErrorReporter errors, Repository repository) {
		fd.bindType(LoadBuiltins.bool);
	}
	
	@Override
	public String toString() {
		return "Function[" + fn.uniqueName() + "]";
	}
}
