package org.flasck.flas.testrunner;

import java.util.ArrayList;
import java.util.List;

import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.commonBase.names.FunctionName;
import org.flasck.flas.commonBase.names.PackageName;
import org.flasck.flas.parsedForm.FunctionCaseDefn;
import org.flasck.flas.parsedForm.Scope;
import org.zinutils.exceptions.NotImplementedException;

public class TestScript implements TestScriptBuilder {
	private final Scope scope;
	private final List<SingleTestCase> cases = new ArrayList<SingleTestCase>();
	private List<TestStep> currentSteps = new ArrayList<TestStep>();
	private int nextStep = 1;
	private String defineInPkg;
	
	public TestScript(String defineInPkg) {
		this.defineInPkg = defineInPkg;
		scope = Scope.topScope(defineInPkg);
	}

	@Override
	public void error(String msg) {
		throw new NotImplementedException(msg);
	}
	

	@Override
	public void addAssert(InputPosition evalPos, Object evalExpr, InputPosition valuePos, Object valueExpr) {
		AssertTestStep as = new AssertTestStep(nextStep, evalPos, evalExpr, valuePos, valueExpr);
		{
			String key = "expr" + nextStep;
			FunctionName fnName = FunctionName.function(evalPos, new PackageName(defineInPkg), key);
			String longName = defineInPkg+"."+key;
			FunctionCaseDefn fn = new FunctionCaseDefn(fnName, new ArrayList<>(), evalExpr);
			fn.provideCaseName(longName+"_0");
			scope.define(key, longName, fn);
		}
		{
			String key = "value" + nextStep;
			FunctionName fnName = FunctionName.function(evalPos, new PackageName(defineInPkg), key);
			String longName = defineInPkg+"."+key;
			FunctionCaseDefn fn = new FunctionCaseDefn(fnName, new ArrayList<>(), valueExpr);
			fn.provideCaseName(longName+"_0");
			scope.define(key, longName, fn);
		}
		nextStep++;
		currentSteps.add(as);
	}

	@Override
	public void addTestCase(String message) {
		cases.add(new SingleTestCase(message, currentSteps));
		currentSteps.clear();
	}

	public Scope scope() {
		return scope;
	}

	public void runAllTests(TestCaseRunner testCaseRunner) {
		for (SingleTestCase tc : cases)
			testCaseRunner.run(tc);
	}
}
