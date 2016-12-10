package test.flas.testrunner;

import org.flasck.flas.testrunner.TestCase;
import org.flasck.flas.testrunner.TestScript;
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
			oneOf(script).add(with(new ValueMatcher(unresolved("x"))));
		}});

		UnitTestConvertor ctor = new UnitTestConvertor(script, "test.golden");
		ctor.convert(CollectionUtils.listOf("\tvalue x", "\t\t32"));
	}

	@Test
	public void testSimpleFunction() {
		TestScriptBuilder script = context.mock(TestScriptBuilder.class);
		context.checking(new Expectations() {{
			oneOf(script).add(with(new ValueMatcher(apply(unresolved("id"), number(420)))));
		}});
		
		UnitTestConvertor ctor = new UnitTestConvertor(script, "test.golden");
		ctor.convert(CollectionUtils.listOf("\tvalue (id 420)", "\t\t420"));
	}

}
