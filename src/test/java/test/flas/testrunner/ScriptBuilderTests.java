package test.flas.testrunner;

import static org.junit.Assert.*;

import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.commonBase.NumericLiteral;
import org.flasck.flas.parsedForm.FunctionCaseDefn;
import org.flasck.flas.parsedForm.Scope;
import org.flasck.flas.parsedForm.Scope.ScopeEntry;
import org.flasck.flas.parsedForm.UnresolvedVar;
import org.flasck.flas.testrunner.AssertTestStep;
import org.flasck.flas.testrunner.TestScript;
import org.junit.Test;

public class ScriptBuilderTests {
	TestScript script = new TestScript("test.golden.script");
	
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
		runUxCase();
		assertEquals("there was not 1 test case", 1, script.cases().size());
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

	private Scope runUxCase() {
		InputPosition posn = new InputPosition("test", 1, 1, null);
		script.add(new AssertTestStep(posn, new UnresolvedVar(posn, "x"), posn, new NumericLiteral(posn, "420", 4)));
		script.addTestCase("test a simple case");
		return script.scope();
	}

}
