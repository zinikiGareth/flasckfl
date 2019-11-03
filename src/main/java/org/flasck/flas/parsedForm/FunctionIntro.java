package org.flasck.flas.parsedForm;

import java.util.ArrayList;
import java.util.List;

import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.commonBase.Pattern;
import org.flasck.flas.commonBase.names.FunctionName;
import org.flasck.flas.parser.FunctionGuardedEquationConsumer;
import org.flasck.flas.patterns.HSITree;
import org.zinutils.exceptions.UtilException;

public class FunctionIntro implements FunctionGuardedEquationConsumer, PatternsHolder {
	public final InputPosition location;
	public final List<Pattern> args;
	private FunctionName fname;
	private final List<FunctionCaseDefn> cases = new ArrayList<>();
	private HSITree hsiTree;

	public FunctionIntro(FunctionName fname, List<Pattern> args) {
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
	public List<Pattern> args() {
		return args;
	}

	@Override
	public void functionCase(FunctionCaseDefn o) {
		cases.add(o);
	}
	
	public List<FunctionCaseDefn> cases() {
		return cases;
	}
	
	public void bindTree(HSITree hsiTree) {
		this.hsiTree = hsiTree;
	}
	
	public HSITree hsiTree() {
		return hsiTree;
	}

	@Override
	public String toString() {
		return "FI[" + name().uniqueName() + "/" + args.size() + " " + cases + "]";
	}
}
