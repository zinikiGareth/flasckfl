package org.flasck.flas.testrunner;

import java.util.ArrayList;
import java.util.List;

import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.commonBase.names.FunctionName;
import org.flasck.flas.commonBase.names.PackageName;
import org.flasck.flas.errors.ErrorReporter;
import org.flasck.flas.parsedForm.CardDefinition;
import org.flasck.flas.parsedForm.FunctionCaseDefn;
import org.flasck.flas.parsedForm.IScope;
import org.flasck.flas.parsedForm.Scope;
import org.flasck.flas.parsedForm.Scope.ScopeEntry;
import org.zinutils.exceptions.UtilException;

public class TestScript implements TestScriptBuilder {
	private final Scope scope;
	private final List<SingleTestCase> cases = new ArrayList<SingleTestCase>();
	private List<TestStep> currentSteps = new ArrayList<TestStep>();
	private int nextStep = 1;
	private String defineInPkg;
	private final ErrorReporter reporter;
	private IScope priorScope;
	
	public TestScript(ErrorReporter errors, IScope inScope, String defineInPkg) {
		this.reporter = errors;
		this.priorScope = inScope;
		this.defineInPkg = defineInPkg;
		this.scope = Scope.topScope(defineInPkg);
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
			FunctionCaseDefn fn = new FunctionCaseDefn(fnName, new ArrayList<>(), evalExpr);
			fn.provideCaseName(0);
			scope.define(key, fn);
		}
		{
			String key = "value" + nextStep;
			FunctionName fnName = FunctionName.function(evalPos, new PackageName(defineInPkg), key);
			FunctionCaseDefn fn = new FunctionCaseDefn(fnName, new ArrayList<>(), valueExpr);
			fn.provideCaseName(0);
			scope.define(key, fn);
		}
		nextStep++;
		currentSteps.add(as);
	}

	
	@Override
	public void addCreate(InputPosition at, String bindVar, String cardType) {
		ScopeEntry se = priorScope.get(cardType);
		if (se == null)
			throw new UtilException("could not find card " + cardType);
		Object cd = se.getValue();
		if (!(cd instanceof CardDefinition))
			throw new UtilException(cardType + " was not a card definition");
		CreateTestStep cs = new CreateTestStep(at, bindVar, ((CardDefinition) cd).cardName);
		currentSteps.add(cs);
	}
	
	@Override
	public void addSend(InputPosition posn, String card, String contract, String method) {
		SendStep step = new SendStep();
		currentSteps.add(step);
	}

	@Override
	public void addMatch(InputPosition posn, WhatToMatch what, String selector, String contents) {
		MatchStep ms = new MatchStep(posn, what, selector, contents);
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
