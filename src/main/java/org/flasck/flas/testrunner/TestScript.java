package org.flasck.flas.testrunner;

import java.util.ArrayList;
import java.util.List;

import org.flasck.flas.parsedForm.FunctionCaseDefn;
import org.flasck.flas.parsedForm.Scope;
import org.flasck.flas.vcode.hsieForm.HSIEForm.CodeType;
import org.zinutils.exceptions.NotImplementedException;

public class TestScript implements TestScriptBuilder {
	private final Scope scope = new Scope(null);
	private final List<SingleTestCase> cases = new ArrayList<SingleTestCase>();
	private List<TestStep> currentSteps = new ArrayList<TestStep>();
	private int nextStep = 1;
	private String defineInPkg;
	
	public TestScript(String defineInPkg) {
		this.defineInPkg = defineInPkg;
	}

	@Override
	public void error(String msg) {
		throw new NotImplementedException(msg);
	}

	@Override
	public void add(AssertTestStep step) {
		step.exprId(nextStep);
		{
			String key = "expr" + nextStep;
			String longName = defineInPkg+"."+key;
			FunctionCaseDefn fn = new FunctionCaseDefn(step.evalPos, CodeType.FUNCTION, longName, new ArrayList<>(), step.eval);
			fn.provideCaseName(longName+"_0");
			scope.define(key, longName, fn);
		}
		{
			String key = "value" + nextStep;
			String longName = defineInPkg+"."+key;
			FunctionCaseDefn fn = new FunctionCaseDefn(step.valuePos, CodeType.FUNCTION, longName, new ArrayList<>(), step.value);
			fn.provideCaseName(longName+"_0");
			scope.define(key, longName, fn);
		}
		nextStep++;
		currentSteps.add(step);
	}

	@Override
	public void addTestCase(String message) {
		cases.add(new SingleTestCase(message, currentSteps));
	}

	public Scope scope() {
		return scope;
	}

	public void runAllTests(TestCaseRunner testCaseRunner) {
		for (SingleTestCase tc : cases)
			testCaseRunner.run(tc);
	}
}
