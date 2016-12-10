package test.flas.testrunner;

import org.flasck.flas.testrunner.TestScriptBuilder;
import org.flasck.flas.testrunner.UnitTestConvertor;
import org.jmock.Expectations;
import org.jmock.integration.junit4.JUnitRuleMockery;
import org.junit.Rule;
import org.junit.Test;
import org.zinutils.collections.CollectionUtils;
import static test.flas.testrunner.ExprMatcher.apply;
import static test.flas.testrunner.ExprMatcher.number;
import static test.flas.testrunner.ExprMatcher.unresolved;

public class UnitTestConvertorTests {
	@Rule public JUnitRuleMockery context = new JUnitRuleMockery();

	@Test
	public void testSimpleVariable() {
		TestScriptBuilder script = context.mock(TestScriptBuilder.class);
		context.checking(new Expectations() {{
			oneOf(script).add(with(new AssertMatcher(unresolved("x"))));
			oneOf(script).addTestCase("the value of x is 32");
		}});

		UnitTestConvertor ctor = new UnitTestConvertor(script, "test.golden");
		ctor.convert(CollectionUtils.listOf("\ttest the value of x is 32", "\t\tassert x", "\t\t\t32"));
	}

	@Test
	public void testSimpleFunction() {
		TestScriptBuilder script = context.mock(TestScriptBuilder.class);
		context.checking(new Expectations() {{
			oneOf(script).add(with(new AssertMatcher(apply(unresolved("id"), number(420)))));
			oneOf(script).addTestCase("id returns what you pass it");
		}});
		
		UnitTestConvertor ctor = new UnitTestConvertor(script, "test.golden");
		ctor.convert(CollectionUtils.listOf("\ttest id returns what you pass it", "\t\tassert (id 420)", "\t\t\t420"));
	}

}
