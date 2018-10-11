package org.flasck.flas.testrunner;

import java.util.ArrayList;
import java.util.List;

import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.commonBase.names.CardName;
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
	public boolean hasErrors() {
		return reporter.hasErrors();
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
		CreateTestStep cs;
		if (cd instanceof CardDefinition) {
			final CardName ct = ((CardDefinition) cd).cardName;
			cs = new CreateTestStep(at, bindVar, ct);
		} else
			throw new UtilException(cardType + " was not a card definition");
		currentSteps.add(cs);
	}
	
	@SuppressWarnings({ "rawtypes", "unchecked" })
	@Override
	public void addSend(InputPosition posn, String card, String contract, String method, List<Object> args, List<Expectation> expecting) {
		List<Integer> posns = new ArrayList<>();
		for (Object o : args) {
			{
				String key = "arg" + nextStep;
				FunctionName fnName = FunctionName.function(posn, new PackageName(defineInPkg), key);
				FunctionCaseDefn fn = new FunctionCaseDefn(fnName, new ArrayList<>(), o);
				fn.provideCaseName(0);
				scope.define(key, fn);
			}
			posns.add(nextStep);
			nextStep++;
		}
		List<Expectation> expects = new ArrayList<>();
		for (Expectation e : expecting) {
			List<Integer> eargs = new ArrayList<>();
			for (Object o : e.args) {
				{
					String key = "earg" + nextStep;
					FunctionName fnName = FunctionName.function(posn, new PackageName(defineInPkg), key);
					FunctionCaseDefn fn = new FunctionCaseDefn(fnName, new ArrayList<>(), o);
					fn.provideCaseName(0);
					scope.define(key, fn);
				}
				eargs.add(nextStep);
				nextStep++;
			}
			expects.add(new Expectation(e.contract, e.method, (List)eargs));
		}
		SendStep step = new SendStep(card, contract, method, posns, expects);
		currentSteps.add(step);
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	@Override
	public void addEvent(InputPosition posn, String card, String method, List<Object> args, List<Expectation> expecting) {
		List<Integer> posns = new ArrayList<>();
		for (Object o : args) {
			{
				String key = "arg" + nextStep;
				FunctionName fnName = FunctionName.function(posn, new PackageName(defineInPkg), key);
				FunctionCaseDefn fn = new FunctionCaseDefn(fnName, new ArrayList<>(), o);
				fn.provideCaseName(0);
				scope.define(key, fn);
			}
			posns.add(nextStep);
			nextStep++;
		}
		List<Expectation> expects = new ArrayList<>();
		for (Expectation e : expecting) {
			List<Integer> eargs = new ArrayList<>();
			for (Object o : e.args) {
				{
					String key = "earg" + nextStep;
					FunctionName fnName = FunctionName.function(posn, new PackageName(defineInPkg), key);
					FunctionCaseDefn fn = new FunctionCaseDefn(fnName, new ArrayList<>(), o);
					fn.provideCaseName(0);
					scope.define(key, fn);
				}
				eargs.add(nextStep);
				nextStep++;
			}
			expects.add(new Expectation(e.contract, e.method, (List)eargs));
		}
		EventStep step = new EventStep(card, method, posns, expects);
		currentSteps.add(step);
	}

	@Override
	public void addMatch(InputPosition posn, HTMLMatcher matcher, String selector) {
		MatchStep ms = new MatchStep(posn, matcher, selector);
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
