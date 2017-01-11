package test.flas.testrunner;

import static test.flas.testrunner.ExprMatcher.number;
import static test.flas.testrunner.ExprMatcher.unresolved;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.flasck.flas.blockForm.Block;
import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.commonBase.StringLiteral;
import org.flasck.flas.testrunner.Expectation;
import org.flasck.flas.testrunner.HTMLMatcher;
import org.flasck.flas.testrunner.TestScriptBuilder;
import org.flasck.flas.testrunner.UnitTestStepConvertor;
import org.flasck.flas.tokenizers.Tokenizable;
import org.hamcrest.Matchers;
import org.jmock.Expectations;
import org.jmock.integration.junit4.JUnitRuleMockery;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.zinutils.collections.CollectionUtils;

public class TestStepConvertorTests {
	@Rule public JUnitRuleMockery context = new JUnitRuleMockery();
	TestScriptBuilder script = context.mock(TestScriptBuilder.class);

	@Before
	public void setup() {
		context.checking(new Expectations() {{
			allowing(script).hasErrors();
		}});
	}
	@Test
	public void testWeCanConvertAScriptStepIntoAnAssertionTest() {
		context.checking(new Expectations() {{
			oneOf(script).addAssert(with(aNonNull(InputPosition.class)), with(unresolved("x")), with(aNonNull(InputPosition.class)), with(number(32)));
		}});

		UnitTestStepConvertor ctor = new UnitTestStepConvertor(script);
		ctor.handle(new Tokenizable("assert x"), CollectionUtils.listOf(new Block(3, "32")));
	}

	@Test
	public void testThanBadStepProducesAnError() {
		context.checking(new Expectations() {{
			oneOf(script).error(with(aNonNull(InputPosition.class)), with("cannot handle input line: throw"));
		}});
		
		UnitTestStepConvertor ctor = new UnitTestStepConvertor(script);
		ctor.handle(new Tokenizable("throw an error"), new ArrayList<>());
	}

	@Test
	public void testWeCanConvertACreateStep() {
		context.checking(new Expectations() {{
			oneOf(script).addCreate(with(aNonNull(InputPosition.class)), with("q"), with("CardName"));
		}});

		UnitTestStepConvertor ctor = new UnitTestStepConvertor(script);
		ctor.handle(new Tokenizable("create q CardName"), new ArrayList<>());
	}

	@Test
	public void testThatCreateWillNotAllowJunkAtTheEndOfTheCommand() {
		context.checking(new Expectations() {{
			oneOf(script).error(with(aNonNull(InputPosition.class)), with("extra characters at end of command: 'xx'"));
		}});

		UnitTestStepConvertor ctor = new UnitTestStepConvertor(script);
		ctor.handle(new Tokenizable("create q CardName xx"), new ArrayList<>());
	}

	@Test
	public void testThatCreateDoesNotHaveNestedBlocks() {
		context.checking(new Expectations() {{
			oneOf(script).error(with(aNonNull(InputPosition.class)), with("create may not have nested instructions"));
		}});

		UnitTestStepConvertor ctor = new UnitTestStepConvertor(script);
		ctor.handle(new Tokenizable("create q CardName"), CollectionUtils.listOf(new Block(3, "property or something")));
	}

	@Test
	public void testWeCanConvertMatchElement() {
		String matchingText = "<div>hello</div>";
		context.checking(new Expectations() {{
			oneOf(script).addMatch(with(aNonNull(InputPosition.class)), with(any(HTMLMatcher.Element.class)), with("div"));
		}});

		UnitTestStepConvertor ctor = new UnitTestStepConvertor(script);
		ctor.handle(new Tokenizable("matchElement div"), Arrays.asList(new Block(3, matchingText)));
	}

	@Test
	public void testWeCanConvertMatchContent() {
		String matchingText = "hello";
		context.checking(new Expectations() {{
			oneOf(script).addMatch(with(aNonNull(InputPosition.class)), with(any(HTMLMatcher.Contents.class)), with("div"));
		}});

		UnitTestStepConvertor ctor = new UnitTestStepConvertor(script);
		ctor.handle(new Tokenizable("matchContents div"), Arrays.asList(new Block(3, matchingText)));
	}

	@Test
	public void testWeCanConvertMatchNoClasses() {
		context.checking(new Expectations() {{
			oneOf(script).addMatch(with(aNonNull(InputPosition.class)), with(any(HTMLMatcher.Class.class)), with("div"));
		}});

		UnitTestStepConvertor ctor = new UnitTestStepConvertor(script);
		ctor.handle(new Tokenizable("matchClass div"), Arrays.asList());
	}

	@Test
	public void testWeCanConvertMatchOneClass() {
		String matchingText = "show";
		context.checking(new Expectations() {{
			oneOf(script).addMatch(with(aNonNull(InputPosition.class)), with(any(HTMLMatcher.Class.class)), with("div"));
		}});

		UnitTestStepConvertor ctor = new UnitTestStepConvertor(script);
		ctor.handle(new Tokenizable("matchClass div"), Arrays.asList(new Block(3, matchingText)));
	}

	@Test
	public void testWeCanConvertMatchTwoClass() {
		String matchingText = "show bright";
		context.checking(new Expectations() {{
			oneOf(script).addMatch(with(aNonNull(InputPosition.class)), with(any(HTMLMatcher.Class.class)), with("div"));
		}});

		UnitTestStepConvertor ctor = new UnitTestStepConvertor(script);
		ctor.handle(new Tokenizable("matchClass div"), Arrays.asList(new Block(3, matchingText)));
	}

	@Test
	public void testWeCanConvertMatchCount() {
		String matchingText = "0";
		context.checking(new Expectations() {{
			oneOf(script).addMatch(with(aNonNull(InputPosition.class)), with(any(HTMLMatcher.Count.class)), with("div"));
		}});

		UnitTestStepConvertor ctor = new UnitTestStepConvertor(script);
		ctor.handle(new Tokenizable("matchCount div"), Arrays.asList(new Block(3, matchingText)));
	}

	@SuppressWarnings("unchecked")
	@Test
	public void testWeCanConvertSendWithNoExpressionsOrExpectations() {
		context.checking(new Expectations() {{
			oneOf(script).addSend(with(aNonNull(InputPosition.class)), with("q"), with("org.flasck.Init"), with("init"), (List<Object>) with(Matchers.empty()), (List<Expectation>) with(Matchers.empty()));
		}});

		UnitTestStepConvertor ctor = new UnitTestStepConvertor(script);
		ctor.handle(new Tokenizable("send q org.flasck.Init init"), Arrays.asList());
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	@Test
	public void testWeCanConvertSendWithASingleExpressionAndNoExpectations() {
		context.checking(new Expectations() {{
			oneOf(script).addSend(with(aNonNull(InputPosition.class)), with("q"), with("org.flasck.Init"), with("init"), (List) with(Matchers.contains(new StringLiteral(null, "hello"))), with(any(List.class)));
		}});

		UnitTestStepConvertor ctor = new UnitTestStepConvertor(script);
		ctor.handle(new Tokenizable("send q org.flasck.Init init 'hello'"), Arrays.asList());
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	@Test
	public void testWeCanConvertSendWithNoExpressionsAndASingleExpectation() {
		context.checking(new Expectations() {{
			oneOf(script).addSend(with(aNonNull(InputPosition.class)), with("q"), with("org.flasck.Init"), with("init"), (List<Object>) with(Matchers.empty()), (List) with(Matchers.contains(new Expectation("Echo", "echoIt", Arrays.asList(new StringLiteral(null, "hello"))))));
		}});

		UnitTestStepConvertor ctor = new UnitTestStepConvertor(script);
		ctor.handle(new Tokenizable("send q org.flasck.Init init"), Arrays.asList(new Block(2, "Echo echoIt 'hello'")));
	}
}
