package test.flas.testrunner;

import static test.flas.testrunner.ExprMatcher.apply;
import static test.flas.testrunner.ExprMatcher.number;
import static test.flas.testrunner.ExprMatcher.string;
import static test.flas.testrunner.ExprMatcher.unresolved;

import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.testrunner.HTMLMatcher;
import org.flasck.flas.testrunner.TestScriptBuilder;
import org.flasck.flas.testrunner.UnitTestConvertor;
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
	
	@Test
	public void testThanBadInputProducesAnError() {
		TestScriptBuilder script = context.mock(TestScriptBuilder.class);
		context.checking(new Expectations() {{
			oneOf(script).error(with(aNonNull(InputPosition.class)), with("cannot handle input line: throw"));
		}});
		
		UnitTestConvertor ctor = new UnitTestConvertor(script);
		ctor.convert(CollectionUtils.listOf("\tthrow an error"));
	}
	
	@Test
	public void testThatAnInvalidStepProducesAnError() {
		TestScriptBuilder script = context.mock(TestScriptBuilder.class);
		context.checking(new Expectations() {{
			oneOf(script).error(with(aNonNull(InputPosition.class)), with("cannot handle input line: throw"));
			oneOf(script).addTestCase("we must have valid input");
		}});
		
		UnitTestConvertor ctor = new UnitTestConvertor(script);
		ctor.convert(CollectionUtils.listOf("\ttest we must have valid input", "\t\tthrow an error"));
	}
	
	@Test
	public void testThatCommentsInMatchAreIgnored() {
		TestScriptBuilder script = context.mock(TestScriptBuilder.class);
		context.checking(new Expectations() {{
			oneOf(script).addMatch(with(aNonNull(InputPosition.class)), with(any(HTMLMatcher.Class.class)), with("div"));
			oneOf(script).addTestCase("comments are ignored");
		}});
		
		UnitTestConvertor ctor = new UnitTestConvertor(script);
		ctor.convert(CollectionUtils.listOf("\ttest comments are ignored", "\t\tmatchClass div", "any comment we choose to make"));
	}
}
