package test.flas.testrunner;

import static test.flas.testrunner.ExprMatcher.apply;
import static test.flas.testrunner.ExprMatcher.number;
import static test.flas.testrunner.ExprMatcher.string;
import static test.flas.testrunner.ExprMatcher.unresolved;

import org.flasck.flas.blockForm.Block;
import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.testrunner.TestScriptBuilder;
import org.flasck.flas.testrunner.UnitTestConvertor;
import org.flasck.flas.testrunner.UnitTestStepConvertor;
import org.flasck.flas.tokenizers.Tokenizable;
import org.jmock.Expectations;
import org.jmock.integration.junit4.JUnitRuleMockery;
import org.junit.Rule;
import org.junit.Test;
import org.zinutils.collections.CollectionUtils;

public class UnitTestConvertorTests {
	@Rule public JUnitRuleMockery context = new JUnitRuleMockery();

	@Test
	public void testWeCanConvertAScriptWithASingleTestCaseGettingIntoAStructuredObject() {
		TestScriptBuilder script = context.mock(TestScriptBuilder.class);
		context.checking(new Expectations() {{
			oneOf(script).addAssert(with(any(InputPosition.class)), with(unresolved("x")), with(any(InputPosition.class)), with(number(32)));
			oneOf(script).addTestCase("the value of x is 32");
		}});

		UnitTestConvertor ctor = new UnitTestConvertor(script);
		ctor.convert(CollectionUtils.listOf("\ttest the value of x is 32", "\t\tassert x", "\t\t\t32"));
	}

	@Test
	public void testWeCanConvertAScriptStepIntoAnAssertionTest() {
		TestScriptBuilder script = context.mock(TestScriptBuilder.class);
		context.checking(new Expectations() {{
			oneOf(script).addAssert(with(aNonNull(InputPosition.class)), with(unresolved("x")), with(aNonNull(InputPosition.class)), with(number(32)));
		}});

		UnitTestStepConvertor ctor = new UnitTestStepConvertor(script);
		ctor.handle(new Tokenizable("assert x"), CollectionUtils.listOf(new Block(3, "32")));
	}

	@SuppressWarnings("unchecked")
	@Test
	public void testSimpleFunction() {
		TestScriptBuilder script = context.mock(TestScriptBuilder.class);
		context.checking(new Expectations() {{
			oneOf(script).addAssert(with(any(InputPosition.class)), with(apply(unresolved("id"), number(420))), with(any(InputPosition.class)), with(number(420)));
			oneOf(script).addTestCase("id returns what you pass it");
		}});
		
		UnitTestConvertor ctor = new UnitTestConvertor(script);
		ctor.convert(CollectionUtils.listOf("\ttest id returns what you pass it", "\t\tassert (id 420)", "\t\t\t420"));
	}


	@SuppressWarnings("unchecked")
	@Test
	public void testWeCanConvertAScriptWithTwoCases() {
		TestScriptBuilder script = context.mock(TestScriptBuilder.class);
		context.checking(new Expectations() {{
			oneOf(script).addAssert(with(any(InputPosition.class)), with(apply(unresolved("id"), number(420))), with(any(InputPosition.class)), with(number(420)));
			oneOf(script).addTestCase("id returns what you pass it");
			oneOf(script).addAssert(with(any(InputPosition.class)), with(apply(unresolved("id"), string("hello"))), with(any(InputPosition.class)), with(string("hello")));
			oneOf(script).addTestCase("id does something else");
		}});
		
		UnitTestConvertor ctor = new UnitTestConvertor(script);
		ctor.convert(CollectionUtils.listOf("\ttest id returns what you pass it", "\t\tassert (id 420)", "\t\t\t420", "\ttest id does something else", "\t\tassert (id 'hello')", "\t\t\t'hello'"));
	}
}
