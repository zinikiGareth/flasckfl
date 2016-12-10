package test.flas.testrunner;

import org.flasck.flas.testrunner.TestCase;
import org.flasck.flas.testrunner.TestScript;
import org.flasck.flas.testrunner.TestScriptBuilder;
import org.flasck.flas.testrunner.UnitTestConvertor;
import org.hamcrest.Matcher;
import org.jmock.Expectations;
import org.jmock.integration.junit4.JUnitRuleMockery;
import org.junit.Rule;
import org.junit.Test;
import org.zinutils.collections.CollectionUtils;

public class UnitTestConvertorTests {
	@Rule public JUnitRuleMockery context = new JUnitRuleMockery();

	@Test
	public void testSimpleVariable() {
		TestScriptBuilder script = context.mock(TestScriptBuilder.class);
		context.checking(new Expectations() {{
			Matcher<Object> tmp = ExprMatcher.unresolved("x");
			oneOf(script).add(with(new ValueMatcher(tmp)));
		}});

		UnitTestConvertor ctor = new UnitTestConvertor(script, "test.golden");
		ctor.convert(CollectionUtils.listOf("\tvalue x", "\t\t32"));
	}

	@Test
	public void testSimpleFunction() {
		UnitTestConvertor ctor = new UnitTestConvertor(new TestScript(), "test.golden");
		ctor.convert(CollectionUtils.listOf("\tvalue (id 420)", "\t\t420"));
//		assertEquals("\texpr1 = (test.golden.id 420)\n\tvalue1 = 420\n", script.flas);
	}

}
