package test.flas.testrunner;

import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.CoreMatchers.isA;
import static org.hamcrest.Matchers.hasProperty;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.commonBase.NumericLiteral;
import org.flasck.flas.errors.ErrorReporter;
import org.flasck.flas.parsedForm.FunctionCaseDefn;
import org.flasck.flas.parsedForm.Scope;
import org.flasck.flas.parsedForm.Scope.ScopeEntry;
import org.flasck.flas.parsedForm.UnresolvedVar;
import org.flasck.flas.testrunner.SingleTestCase;
import org.flasck.flas.testrunner.TestCaseRunner;
import org.flasck.flas.testrunner.TestScript;
import org.jmock.Expectations;
import org.jmock.integration.junit4.JUnitRuleMockery;
import org.junit.Rule;
import org.junit.Test;

public class ScriptBuilderTests {
	@Rule public JUnitRuleMockery context = new JUnitRuleMockery();
	String TEST_CASE_NAME = "test a simple case";
	ErrorReporter reporter = context.mock(ErrorReporter.class);
	TestScript script = new TestScript(reporter, "test.golden.script");
	InputPosition posn = new InputPosition("test", 1, 1, null);
	
	@Test
	public void testABuilderHasANonNullScope() {
		Scope scope = script.scope();
		assertNotNull("the scope was not defined", scope);
		assertEquals("there were items already in the scope", 0, scope.size());
	}

	@Test
	public void testASimpleCaseHasTwoItemsInTheScope() {
		Scope scope = runUxCase();
		assertNotNull(scope);
		assertEquals("there were not two items in the scope", 2, scope.size());
	}

	@Test
	public void testASimpleCaseHasATestCaseItem() {
		TestCaseRunner it = context.mock(TestCaseRunner.class);
		context.checking(new Expectations() {{
			oneOf(it).run(with(allOf(isA(SingleTestCase.class), hasProperty("description"))));
		}});
		runUxCase();
		script.runAllTests(it);
	}

	@Test
	public void testTheTestCaseObjectHasTheRightName() {
		runUxCase();
//		assertEquals("the test case did not have the right name", TEST_CASE_NAME, script.cases().get(0).description());
	}

	@Test
	public void testTheTestCaseObjectHasOneStep() {
		runUxCase();
//		assertEquals("the test case did not have exactly one step", 1, script.cases().get(0).steps().size());
	}

	@Test
	public void testTheFirstStepIsAssert1() {
		runUxCase();
//		assertTrue("the first step was not an assert", script.cases().get(0).steps().get(0) instanceof AssertTestStep);
//		assertEquals("the test case did not have exactly one step", 1, ((AssertTestStep)script.cases().get(0).steps().get(0)).exprId());
	}

	@Test
	public void testExpr1IsInTheSimpleScript() {
		Scope scope = runUxCase();
		assertTrue("expr1 was not in the scope", scope.contains("expr1"));
	}

	@Test
	public void testExpr1IsAFunction() {
		Scope scope = runUxCase();
		ScopeEntry se = scope.get("expr1");
		assertNotNull("did not find expr1", se);
		assertNotNull("entry was null", se.getValue());
		assertTrue("not a function", se.getValue() instanceof FunctionCaseDefn);
	}

	@Test
	public void testExpr1HasTheRightExpression() {
		Scope scope = runUxCase();
		ScopeEntry se = scope.get("expr1");
		assertTrue("expr not a var", ((FunctionCaseDefn)se.getValue()).expr instanceof UnresolvedVar);
		assertTrue("var was not 'x'", ((UnresolvedVar)((FunctionCaseDefn)se.getValue()).expr).var.equals("x"));
	}

	@Test
	public void testValue1IsInTheSimpleScript() {
		Scope scope = runUxCase();
		assertTrue("expr1 was not in the scope", scope.contains("value1"));
	}


	@Test
	public void testValue1IsAFunction() {
		Scope scope = runUxCase();
		ScopeEntry se = scope.get("value1");
		assertNotNull("did not find value1", se);
		assertNotNull("entry was null", se.getValue());
		assertTrue("not a function", se.getValue() instanceof FunctionCaseDefn);
	}

	@Test
	public void testValue1HasTheRightExpression() {
		Scope scope = runUxCase();
		ScopeEntry se = scope.get("value1");
		assertTrue("expr not a var", ((FunctionCaseDefn)se.getValue()).expr instanceof NumericLiteral);
		assertTrue("var was not 'x'", ((NumericLiteral)((FunctionCaseDefn)se.getValue()).expr).text.equals("420"));
	}

	@Test
	public void testAddingTwoCasesOnlyHasOneStepInEachCase() {
		runUxCase();
		runUxCase();
		script.runAllTests(new TestCaseRunner() {
			
			@Override
			public void run(SingleTestCase tc) {
				tc.assertStepCount(1);
			}
		});
	}

	@Test
	public void testItRaisesAnErrorIfWeCallError() {
		String errorMsg = "there is no command 'foo'";
		context.checking(new Expectations() {{
			oneOf(reporter).message(posn, errorMsg);
		}});
		script.error(posn, errorMsg);
	}

	private Scope runUxCase() {
		script.addAssert(posn, new UnresolvedVar(posn, "x"), posn, new NumericLiteral(posn, "420", 4));
		script.addTestCase(TEST_CASE_NAME);
		return script.scope();
	}

}
