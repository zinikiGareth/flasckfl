package test.parsing.ut;

import static org.junit.Assert.assertTrue;

import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.commonBase.Expr;
import org.flasck.flas.commonBase.StringLiteral;
import org.flasck.flas.commonBase.names.FunctionName;
import org.flasck.flas.commonBase.names.PackageName;
import org.flasck.flas.errors.ErrorReporter;
import org.flasck.flas.parsedForm.TypeReference;
import org.flasck.flas.parsedForm.UnresolvedVar;
import org.flasck.flas.parser.IgnoreNestedParser;
import org.flasck.flas.parser.NoNestingParser;
import org.flasck.flas.parser.TDAParsing;
import org.flasck.flas.parser.ut.SingleExpressionParser;
import org.flasck.flas.parser.ut.TestStepParser;
import org.flasck.flas.parser.ut.UnitDataDeclaration;
import org.flasck.flas.parser.ut.UnitTestDefinitionConsumer;
import org.flasck.flas.parser.ut.UnitTestNamer;
import org.flasck.flas.parser.ut.UnitTestStepConsumer;
import org.flasck.flas.tokenizers.Tokenizable;
import org.jmock.Expectations;
import org.jmock.integration.junit4.JUnitRuleMockery;
import org.junit.Rule;
import org.junit.Test;

import test.parsing.ExprMatcher;
import test.parsing.LocalErrorTracker;
import test.parsing.TypeReferenceMatcher;

public class UnitTestStepParsingTests {
	@Rule public JUnitRuleMockery context = new JUnitRuleMockery();
	private ErrorReporter errors = context.mock(ErrorReporter.class);
	private LocalErrorTracker tracker = new LocalErrorTracker(errors);
	private UnitTestNamer namer = context.mock(UnitTestNamer.class);
	private UnitTestDefinitionConsumer topLevel = context.mock(UnitTestDefinitionConsumer.class);
	private UnitTestStepConsumer builder = context.mock(UnitTestStepConsumer.class);
	private final PackageName pkg = new PackageName("test.pkg._ut_file");
	private InputPosition pos = new InputPosition("fred", 10, 0, "hello");

	@Test
	public void testWeCanHandleASimpleAssertStep() {
		context.checking(new Expectations() {{
			oneOf(builder).assertion(with(ExprMatcher.unresolved("x")), with(ExprMatcher.number(86)));
		}});
		TestStepParser utp = new TestStepParser(tracker, namer, builder, topLevel);
		TDAParsing nested = utp.tryParsing(UnitTestTopLevelParsingTests.line("assert x"));
		assertTrue(nested instanceof SingleExpressionParser);
		TDAParsing nnp = nested.tryParsing(UnitTestTopLevelParsingTests.line("86"));
		assertTrue(nnp instanceof NoNestingParser);
		nnp.scopeComplete(pos);
		nested.scopeComplete(pos);
		utp.scopeComplete(pos);
	}
	
	@Test
	public void testThatThereIsAnExpressionOnTheLine() {
		final Tokenizable line = UnitTestTopLevelParsingTests.line("assert");
		context.checking(new Expectations() {{
			oneOf(errors).message(line, "assert requires expression to evaluate");
		}});
		TestStepParser utp = new TestStepParser(tracker, namer, builder, topLevel);
		TDAParsing nested = utp.tryParsing(line);
		assertTrue(nested instanceof IgnoreNestedParser);
	}
	
	@Test
	public void testThatThereIsAMatchExpression() {
		final Tokenizable line = UnitTestTopLevelParsingTests.line("assert x");
		context.checking(new Expectations() {{
			oneOf(errors).message(pos, "assert requires exactly one match expression");
		}});
		TestStepParser utp = new TestStepParser(tracker, namer, builder, topLevel);
		TDAParsing nested = utp.tryParsing(line);
		assertTrue(nested instanceof SingleExpressionParser);
		nested.scopeComplete(pos);
	}
	
	@Test
	public void testThatThereIsOnlyOneExpressionOnTheLine() {
		final Tokenizable line = UnitTestTopLevelParsingTests.line("assert 42)");
		context.checking(new Expectations() {{
			oneOf(errors).message(with(any(InputPosition.class)), with("invalid tokens after expression"));
		}});
		TestStepParser utp = new TestStepParser(tracker, namer, builder, topLevel);
		TDAParsing nested = utp.tryParsing(line);
		assertTrue(nested instanceof IgnoreNestedParser);
	}
	
	@Test
	public void testWeCanOnlyHandleOneMatchingExpression() {
		context.checking(new Expectations() {{
			oneOf(builder).assertion(with(ExprMatcher.unresolved("x")), with(ExprMatcher.number(86)));
			oneOf(errors).message(pos, "assert requires exactly one match expression");
		}});
		TestStepParser utp = new TestStepParser(tracker, namer, builder, topLevel);
		TDAParsing nested = utp.tryParsing(UnitTestTopLevelParsingTests.line("assert x"));
		assertTrue(nested instanceof SingleExpressionParser);
		nested.tryParsing(UnitTestTopLevelParsingTests.line("86"));
		nested.tryParsing(UnitTestTopLevelParsingTests.line("32"));
		nested.scopeComplete(pos);
		utp.scopeComplete(pos);
	}

	@Test
	public void testWeCanHandleASimpleEventStep() {
		context.checking(new Expectations() {{
			oneOf(builder).event((UnresolvedVar)with(ExprMatcher.unresolved("card")), (StringLiteral) with(ExprMatcher.string("click")), with(ExprMatcher.apply(ExprMatcher.unresolved("ClickEvent"), ExprMatcher.apply(ExprMatcher.operator("{}"), any(Expr.class), any(Expr.class)))));
		}});
		TestStepParser utp = new TestStepParser(tracker, namer, builder, topLevel);
		TDAParsing nested = utp.tryParsing(UnitTestTopLevelParsingTests.line("event card click (ClickEvent { x: 42, y: 31 })"));
		assertTrue(nested instanceof NoNestingParser);
		nested.scopeComplete(pos);
		utp.scopeComplete(pos);
	}
	
	@Test
	public void testEventNamesCanHaveHyphens() {
		context.checking(new Expectations() {{
			oneOf(builder).event((UnresolvedVar)with(ExprMatcher.unresolved("card")), (StringLiteral) with(ExprMatcher.string("double-click")), with(ExprMatcher.apply(ExprMatcher.unresolved("ClickEvent"), ExprMatcher.apply(ExprMatcher.operator("{}"), any(Expr.class), any(Expr.class)))));
		}});
		TestStepParser utp = new TestStepParser(tracker, namer, builder, topLevel);
		TDAParsing nested = utp.tryParsing(UnitTestTopLevelParsingTests.line("event card double-click (ClickEvent { x: 42, y: 31 })"));
		assertTrue(nested instanceof NoNestingParser);
		nested.scopeComplete(pos);
		utp.scopeComplete(pos);
	}
	
	@Test
	public void testAnEventNeedsMoreThanJustAKeyword() {
		final Tokenizable toks = UnitTestTopLevelParsingTests.line("event");
		context.checking(new Expectations() {{
			oneOf(errors).message(toks, "missing arguments");
		}});
		TestStepParser utp = new TestStepParser(tracker, namer, builder, topLevel);
		TDAParsing nested = utp.tryParsing(toks);
		assertTrue(nested instanceof IgnoreNestedParser);
		nested.scopeComplete(pos);
		utp.scopeComplete(pos);
	}
	
	@Test
	public void testAnEventNeedsEverything() {
		final Tokenizable toks = UnitTestTopLevelParsingTests.line("event myCard click");
		context.checking(new Expectations() {{
			oneOf(errors).message(toks, "missing arguments");
		}});
		TestStepParser utp = new TestStepParser(tracker, namer, builder, topLevel);
		TDAParsing nested = utp.tryParsing(toks);
		assertTrue(nested instanceof IgnoreNestedParser);
		nested.scopeComplete(pos);
		utp.scopeComplete(pos);
	}
	
	@Test
	public void testWeCanHandleASendStep() {
		context.checking(new Expectations() {{
			oneOf(builder).send((UnresolvedVar)with(ExprMatcher.unresolved("card")), (TypeReference) with(TypeReferenceMatcher.type("SomeContract")), with(ExprMatcher.apply(ExprMatcher.unresolved("method"), ExprMatcher.unresolved("true"), ExprMatcher.number(86), ExprMatcher.string("hello"))));
		}});
		TestStepParser utp = new TestStepParser(tracker, namer, builder, topLevel);
		TDAParsing nested = utp.tryParsing(UnitTestTopLevelParsingTests.line("send card SomeContract method true 86 'hello'"));
		assertTrue(nested instanceof NoNestingParser);
		nested.scopeComplete(pos);
		utp.scopeComplete(pos);
	}
	
	@Test
	public void testWeCanHandleASendStepWithNoArgumentsToTheMethod() {
		context.checking(new Expectations() {{
			oneOf(builder).send((UnresolvedVar)with(ExprMatcher.unresolved("card")), (TypeReference) with(TypeReferenceMatcher.type("SomeContract")), with(ExprMatcher.unresolved("method")));
		}});
		TestStepParser utp = new TestStepParser(tracker, namer, builder, topLevel);
		TDAParsing nested = utp.tryParsing(UnitTestTopLevelParsingTests.line("send card SomeContract method"));
		assertTrue(nested instanceof NoNestingParser);
		nested.scopeComplete(pos);
		utp.scopeComplete(pos);
	}
	
	@Test
	public void testASendStepNeedsEverything() {
		final Tokenizable toks = UnitTestTopLevelParsingTests.line("send card SomeContract");
		context.checking(new Expectations() {{
			oneOf(errors).message(toks, "missing arguments");
		}});
		TestStepParser utp = new TestStepParser(tracker, namer, builder, topLevel);
		TDAParsing nested = utp.tryParsing(toks);
		assertTrue(nested instanceof IgnoreNestedParser);
		nested.scopeComplete(pos);
		utp.scopeComplete(pos);
	}
	
	@Test
	public void testWeCanHandleATemplateStep() {
		context.checking(new Expectations() {{
			oneOf(builder).template();
		}});
		TestStepParser utp = new TestStepParser(tracker, namer, builder, topLevel);
		TDAParsing nested = utp.tryParsing(UnitTestTopLevelParsingTests.line("template"));
		assertTrue(nested instanceof NoNestingParser);
		nested.scopeComplete(pos);
		utp.scopeComplete(pos);
	}
	
	@Test
	public void testWeCanHandleADataDefinitionStep() {
		context.checking(new Expectations() {{
			oneOf(namer).dataName(with(any(InputPosition.class)), with("x")); will(returnValue(FunctionName.function(pos, pkg, "x")));
			oneOf(builder).data(with(any(UnitDataDeclaration.class)));
			oneOf(topLevel).data(with(any(UnitDataDeclaration.class)));
		}});
		TestStepParser utp = new TestStepParser(tracker, namer, builder, topLevel);
		TDAParsing nested = utp.tryParsing(UnitTestTopLevelParsingTests.line("data Number x <- 86"));
		assertTrue(nested instanceof NoNestingParser);
		nested.scopeComplete(pos);
		utp.scopeComplete(pos);
	}
	
	@Test
	public void testWeObjectToUnknownSteps() {
		final Tokenizable line = UnitTestTopLevelParsingTests.line("foo");
		context.checking(new Expectations() {{
			oneOf(errors).message(line, "unrecognized test step foo");
		}});
		TestStepParser utp = new TestStepParser(tracker, namer, builder, topLevel);
		TDAParsing nested = utp.tryParsing(line);
		assertTrue(nested instanceof IgnoreNestedParser);
		nested.scopeComplete(pos);
		utp.scopeComplete(pos);
	}
	
	@Test
	public void testWeObjectToGarbage() {
		final Tokenizable line = UnitTestTopLevelParsingTests.line("++3");
		context.checking(new Expectations() {{
			oneOf(errors).message(line, "syntax error");
		}});
		TestStepParser utp = new TestStepParser(tracker, namer, builder, topLevel);
		TDAParsing nested = utp.tryParsing(line);
		assertTrue(nested instanceof IgnoreNestedParser);
		nested.scopeComplete(pos);
		utp.scopeComplete(pos);
	}
}
