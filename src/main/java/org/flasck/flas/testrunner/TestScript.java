package org.flasck.flas.testrunner;

import java.util.ArrayList;
import java.util.List;

import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.commonBase.names.FunctionName;
import org.flasck.flas.commonBase.names.PackageName;
import org.flasck.flas.errors.ErrorReporter;
import org.flasck.flas.parsedForm.FunctionCaseDefn;
import org.flasck.flas.parsedForm.Scope;

public class TestScript implements TestScriptBuilder {
	private final Scope scope;
	private final List<SingleTestCase> cases = new ArrayList<SingleTestCase>();
	private List<TestStep> currentSteps = new ArrayList<TestStep>();
	private int nextStep = 1;
	private String defineInPkg;
	private final ErrorReporter reporter;
	
	public TestScript(ErrorReporter errors, String defineInPkg) {
		this.reporter = errors;
		this.defineInPkg = defineInPkg;
		scope = Scope.topScope(defineInPkg);
	}

	@Override
	public void error(InputPosition posn, String msg) {
		reporter.message(posn, msg);
	}
	

	@Override
	public void addAssert(InputPosition evalPos, Object evalExpr, InputPosition valuePos, Object valueExpr) {
		AssertTestStep as = new AssertTestStep(nextStep, evalPos, evalExpr, valuePos, valueExpr);
		{
			String key = "expr" + nextStep;
			FunctionName fnName = FunctionName.function(evalPos, new PackageName(defineInPkg), key);
			String longName = defineInPkg+"."+key;
			FunctionCaseDefn fn = new FunctionCaseDefn(fnName, new ArrayList<>(), evalExpr);
			fn.provideCaseName(0);
			scope.define(key, longName, fn);
		}
		{
			String key = "value" + nextStep;
			FunctionName fnName = FunctionName.function(evalPos, new PackageName(defineInPkg), key);
			String longName = defineInPkg+"."+key;
			FunctionCaseDefn fn = new FunctionCaseDefn(fnName, new ArrayList<>(), valueExpr);
			fn.provideCaseName(0);
			scope.define(key, longName, fn);
		}
		nextStep++;
		currentSteps.add(as);
	}

	
	@Override
	public void addCreate(InputPosition at, String bindVar, String cardType) {
		CreateTestStep cs = new CreateTestStep(at, bindVar, cardType);
		currentSteps.add(cs);
	}
	
	@Override
	public void addMatchElement(InputPosition posn, String onCard, String selector, String contents) {
		MatchElementStep ms = new MatchElementStep(posn, onCard, selector, contents);
		currentSteps.add(ms);
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
