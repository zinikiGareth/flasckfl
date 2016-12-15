package test.flas.testrunner;

import static test.flas.testrunner.ExprMatcher.number;
import static test.flas.testrunner.ExprMatcher.unresolved;

import java.util.ArrayList;

import org.flasck.flas.blockForm.Block;
import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.testrunner.TestScriptBuilder;
import org.flasck.flas.testrunner.UnitTestStepConvertor;
import org.flasck.flas.tokenizers.Tokenizable;
import org.jmock.Expectations;
import org.jmock.integration.junit4.JUnitRuleMockery;
import org.junit.Rule;
import org.junit.Test;
import org.zinutils.collections.CollectionUtils;

public class TestStepConvertorTests {
	@Rule public JUnitRuleMockery context = new JUnitRuleMockery();

	@Test
	public void testWeCanConvertAScriptStepIntoAnAssertionTest() {
		TestScriptBuilder script = context.mock(TestScriptBuilder.class);
		context.checking(new Expectations() {{
			oneOf(script).addAssert(with(aNonNull(InputPosition.class)), with(unresolved("x")), with(aNonNull(InputPosition.class)), with(number(32)));
		}});

		UnitTestStepConvertor ctor = new UnitTestStepConvertor(script);
		ctor.handle(new Tokenizable("assert x"), CollectionUtils.listOf(new Block(3, "32")));
	}

	@Test
	public void testThanBadStepProducesAnError() {
		TestScriptBuilder script = context.mock(TestScriptBuilder.class);
		context.checking(new Expectations() {{
			oneOf(script).error(with(aNonNull(InputPosition.class)), with("cannot handle input line: throw"));
		}});
		
		UnitTestStepConvertor ctor = new UnitTestStepConvertor(script);
		ctor.handle(new Tokenizable("throw an error"), new ArrayList<>());
	}
}
