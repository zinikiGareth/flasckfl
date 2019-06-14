package org.flasck.flas.parsedForm;

import java.util.ArrayList;
import java.util.List;

import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.commonBase.names.FunctionName;
import org.flasck.flas.parser.FunctionGuardedEquationConsumer;
import org.zinutils.exceptions.UtilException;

public class FunctionIntro implements FunctionGuardedEquationConsumer {
	public final InputPosition location;
	public final List<Object> args;
	private FunctionName fname;
	private final List<FunctionCaseDefn> cases = new ArrayList<>();

	public FunctionIntro(FunctionName fname, List<Object> args) {
		this.location = fname.location;
		this.fname = fname;
		this.args = args;
	}

	public FunctionName name() {
		if (fname == null)
			throw new UtilException("Deprecated");
		return fname;
	}

	@Override
	public void functionCase(FunctionCaseDefn o) {
		cases.add(o);
	}
	
	public List<FunctionCaseDefn> cases() {
		return cases;
	}

	@Override
	public String toString() {
		return "FI[" + name().uniqueName() + "/" + args.size() + "]";
	}
}
