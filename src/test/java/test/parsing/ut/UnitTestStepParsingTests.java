package test.parsing.ut;

import static org.junit.Assert.assertTrue;

import java.util.List;

import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.commonBase.Expr;
import org.flasck.flas.commonBase.names.FunctionName;
import org.flasck.flas.commonBase.names.PackageName;
import org.flasck.flas.errors.ErrorReporter;
import org.flasck.flas.parsedForm.AnonymousVar;
import org.flasck.flas.parsedForm.TypeReference;
import org.flasck.flas.parsedForm.UnresolvedVar;
import org.flasck.flas.parsedForm.ut.MatchedItem;
import org.flasck.flas.parser.IgnoreNestedParser;
import org.flasck.flas.parser.NoNestingParser;
import org.flasck.flas.parser.TDAParsing;
import org.flasck.flas.parser.ut.FreeTextParser;
import org.flasck.flas.parser.ut.SingleExpressionParser;
import org.flasck.flas.parser.ut.TestStepParser;
import org.flasck.flas.parser.ut.UnitDataDeclaration;
import org.flasck.flas.parser.ut.UnitTestDefinitionConsumer;
import org.flasck.flas.parser.ut.UnitTestNamer;
import org.flasck.flas.parser.ut.UnitTestStepConsumer;
import org.flasck.flas.stories.TDAMultiParser;
import org.flasck.flas.tokenizers.Tokenizable;
import org.hamcrest.Matchers;
import org.jmock.Expectations;
import org.jmock.integration.junit4.JUnitRuleMockery;
import org.junit.Rule;
import org.junit.Test;

import flas.matchers.ExprMatcher;
import flas.matchers.TargetZoneMatcher;
import flas.matchers.TypeReferenceMatcher;
import test.parsing.LocalErrorTracker;

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

	@SuppressWarnings("unchecked")
	@Test
	public void testWeCanHandleASimpleShoveStep() {
		context.checking(new Expectations() {{
			oneOf(builder).shove((List<UnresolvedVar>) with(Matchers.contains(ExprMatcher.unresolved("card"), ExprMatcher.unresolved("x"))), with(ExprMatcher.number(86)));
		}});
		TestStepParser utp = new TestStepParser(tracker, namer, builder, topLevel);
		TDAParsing nested = utp.tryParsing(UnitTestTopLevelParsingTests.line("shove card.x"));
		assertTrue(nested instanceof SingleExpressionParser);
		TDAParsing nnp = nested.tryParsing(UnitTestTopLevelParsingTests.line("86"));
		assertTrue(nnp instanceof NoNestingParser);
		nnp.scopeComplete(pos);
		nested.scopeComplete(pos);
		utp.scopeComplete(pos);
	}
	
	@Test
	public void testShoveNeedsSomething() {
		Tokenizable toks = UnitTestTopLevelParsingTests.line("shove");
		context.checking(new Expectations() {{
			oneOf(errors).message(toks, "field path expected");
		}});
		TestStepParser utp = new TestStepParser(tracker, namer, builder, topLevel);
		TDAParsing nested = utp.tryParsing(toks);
		assertTrue(nested instanceof IgnoreNestedParser);
	}
	
	@Test
	public void testShoveNeedsMoreThanJustOneSymbol() {
		Tokenizable toks = UnitTestTopLevelParsingTests.line("shove card");
		context.checking(new Expectations() {{
			oneOf(errors).message(toks, ". expected");
		}});
		TestStepParser utp = new TestStepParser(tracker, namer, builder, topLevel);
		TDAParsing nested = utp.tryParsing(toks);
		assertTrue(nested instanceof IgnoreNestedParser);
	}
	
	@Test
	public void testShoveExprCannotEndOnADot() {
		Tokenizable toks = UnitTestTopLevelParsingTests.line("shove card.");
		context.checking(new Expectations() {{
			oneOf(errors).message(toks, "field path expected");
		}});
		TestStepParser utp = new TestStepParser(tracker, namer, builder, topLevel);
		TDAParsing nested = utp.tryParsing(toks);
		assertTrue(nested instanceof IgnoreNestedParser);
	}
	
	@Test
	public void testShoveExprCannotHaveDollarNotDot() {
		Tokenizable toks = UnitTestTopLevelParsingTests.line("shove card$");
		context.checking(new Expectations() {{
			oneOf(errors).message(toks, ". expected");
		}});
		TestStepParser utp = new TestStepParser(tracker, namer, builder, topLevel);
		TDAParsing nested = utp.tryParsing(toks);
		assertTrue(nested instanceof IgnoreNestedParser);
	}

	@Test
	public void testShoveExprCannotBeANumber() {
		Tokenizable toks = UnitTestTopLevelParsingTests.line("shove card.3");
		context.checking(new Expectations() {{
			oneOf(errors).message(toks, "field path expected");
		}});
		TestStepParser utp = new TestStepParser(tracker, namer, builder, topLevel);
		TDAParsing nested = utp.tryParsing(toks);
		assertTrue(nested instanceof IgnoreNestedParser);
	}

	@Test
	public void testShoveExprCannotHaveTwoDots() {
		Tokenizable toks = UnitTestTopLevelParsingTests.line("shove card..");
		context.checking(new Expectations() {{
			oneOf(errors).message(toks, "field path expected");
		}});
		TestStepParser utp = new TestStepParser(tracker, namer, builder, topLevel);
		TDAParsing nested = utp.tryParsing(toks);
		assertTrue(nested instanceof IgnoreNestedParser);
	}

	@Test
	public void testWeCanHandleASimpleEventStep() {
		context.checking(new Expectations() {{
			oneOf(builder).event((UnresolvedVar)with(ExprMatcher.unresolved("card")), with(TargetZoneMatcher.path("target")), with(ExprMatcher.apply(ExprMatcher.typeref("ClickEvent"), ExprMatcher.apply(ExprMatcher.operator("{}"), any(Expr.class), any(Expr.class)))));
		}});
		TestStepParser utp = new TestStepParser(tracker, namer, builder, topLevel);
		TDAParsing nested = utp.tryParsing(UnitTestTopLevelParsingTests.line("event card target (ClickEvent { x: 42, y: 31 })"));
		assertTrue(nested instanceof NoNestingParser);
		nested.scopeComplete(pos);
		utp.scopeComplete(pos);
	}
	
	@SuppressWarnings("unchecked")
	@Test
	public void testWeCanHandleAnExpectationStep() {
		context.checking(new Expectations() {{
			oneOf(builder).expect((UnresolvedVar)with(ExprMatcher.unresolved("svc")),
					(UnresolvedVar) with(ExprMatcher.unresolved("meth")),
					(Expr[])with(Matchers.array(ExprMatcher.number(22),
							ExprMatcher.apply(ExprMatcher.unresolved("length"), ExprMatcher.string("hello")))),
					with(any(AnonymousVar.class)));
		}});
		TestStepParser utp = new TestStepParser(tracker, namer, builder, topLevel);
		TDAParsing nested = utp.tryParsing(UnitTestTopLevelParsingTests.line("expect svc meth 22 (length 'hello')"));
		assertTrue(nested instanceof TDAMultiParser);
		nested.scopeComplete(pos);
		utp.scopeComplete(pos);
	}
	
	@Test
	public void testAnEventNeedsMoreThanJustAKeyword() {
		final Tokenizable toks = UnitTestTopLevelParsingTests.line("event");
		context.checking(new Expectations() {{
			oneOf(errors).message(toks, "must specify a card to receive event");
		}});
		TestStepParser utp = new TestStepParser(tracker, namer, builder, topLevel);
		TDAParsing nested = utp.tryParsing(toks);
		assertTrue(nested instanceof IgnoreNestedParser);
		nested.scopeComplete(pos);
		utp.scopeComplete(pos);
	}
	
	@Test
	public void testAnEventNeedsATarget() {
		final Tokenizable toks = UnitTestTopLevelParsingTests.line("event myCard");
		context.checking(new Expectations() {{
			oneOf(errors).message(toks, "valid target zone expected");
		}});
		TestStepParser utp = new TestStepParser(tracker, namer, builder, topLevel);
		TDAParsing nested = utp.tryParsing(toks);
		assertTrue(nested instanceof IgnoreNestedParser);
		nested.scopeComplete(pos);
		utp.scopeComplete(pos);
	}
	
	@Test
	public void testAnEventNeedsEverything() {
		final Tokenizable toks = UnitTestTopLevelParsingTests.line("event myCard zone");
		context.checking(new Expectations() {{
			oneOf(errors).message(toks, "must provide an event object");
		}});
		TestStepParser utp = new TestStepParser(tracker, namer, builder, topLevel);
		TDAParsing nested = utp.tryParsing(toks);
		assertTrue(nested instanceof IgnoreNestedParser);
		nested.scopeComplete(pos);
		utp.scopeComplete(pos);
	}
	
	@Test
	public void testWeCanHandleAContractStep() {
		context.checking(new Expectations() {{
			oneOf(builder).sendOnContract((UnresolvedVar)with(ExprMatcher.unresolved("card")), (TypeReference) with(TypeReferenceMatcher.type("SomeContract")), with(ExprMatcher.apply(ExprMatcher.unresolved("method"), ExprMatcher.unresolved("true"), ExprMatcher.number(86), ExprMatcher.string("hello"))));
		}});
		TestStepParser utp = new TestStepParser(tracker, namer, builder, topLevel);
		TDAParsing nested = utp.tryParsing(UnitTestTopLevelParsingTests.line("contract card SomeContract method true 86 'hello'"));
		assertTrue(nested instanceof NoNestingParser);
		nested.scopeComplete(pos);
		utp.scopeComplete(pos);
	}
	
	@Test
	public void testWeCanHandleAContractStepWithNoArgumentsToTheMethod() {
		context.checking(new Expectations() {{
			oneOf(builder).sendOnContract((UnresolvedVar)with(ExprMatcher.unresolved("card")), (TypeReference) with(TypeReferenceMatcher.type("SomeContract")), with(ExprMatcher.unresolved("method")));
		}});
		TestStepParser utp = new TestStepParser(tracker, namer, builder, topLevel);
		TDAParsing nested = utp.tryParsing(UnitTestTopLevelParsingTests.line("contract card SomeContract method"));
		assertTrue(nested instanceof NoNestingParser);
		nested.scopeComplete(pos);
		utp.scopeComplete(pos);
	}
	
	@Test
	public void testAContractStepNeedsEverything() {
		final Tokenizable toks = UnitTestTopLevelParsingTests.line("contract card SomeContract");
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
	public void testWeCanHandleAnInvokeStep() {
		context.checking(new Expectations() {{
			oneOf(builder).invokeObjectMethod(with(ExprMatcher.apply(ExprMatcher.member(ExprMatcher.unresolved("obj"), ExprMatcher.unresolved("meth")), ExprMatcher.string("hello"))));
		}});
		TestStepParser utp = new TestStepParser(tracker, namer, builder, topLevel);
		TDAParsing nested = utp.tryParsing(UnitTestTopLevelParsingTests.line("invoke obj.meth 'hello'"));
		assertTrue(nested instanceof NoNestingParser);
		nested.scopeComplete(pos);
		utp.scopeComplete(pos);
	}

	@Test
	public void testWeCanHandleAMatchStep() {
		context.checking(new Expectations() {{
			oneOf(builder).match((UnresolvedVar)with(ExprMatcher.unresolved("card")), with(MatchedItem.TEXT), with(TargetZoneMatcher.path()), with(false), with("hello"));
		}});
		TestStepParser utp = new TestStepParser(tracker, namer, builder, topLevel);
		TDAParsing nested = utp.tryParsing(UnitTestTopLevelParsingTests.line("match card text"));
		assertTrue(nested instanceof FreeTextParser);
		nested.tryParsing(UnitTestTopLevelParsingTests.line("hello"));
		nested.scopeComplete(pos);
		utp.scopeComplete(pos);
	}
	
	@Test
	public void testWeCanMatchStyles() {
		context.checking(new Expectations() {{
			oneOf(builder).match((UnresolvedVar)with(ExprMatcher.unresolved("h")), with(MatchedItem.STYLE), with(TargetZoneMatcher.path()), with(false), with("hello"));
		}});
		TestStepParser utp = new TestStepParser(tracker, namer, builder, topLevel);
		TDAParsing nested = utp.tryParsing(UnitTestTopLevelParsingTests.line("match h style"));
		assertTrue(nested instanceof FreeTextParser);
		nested.tryParsing(UnitTestTopLevelParsingTests.line("hello"));
		nested.scopeComplete(pos);
		utp.scopeComplete(pos);
	}
	
	@Test
	public void testWeCanMatchWithASelector() {
		context.checking(new Expectations() {{
			oneOf(builder).match((UnresolvedVar)with(ExprMatcher.unresolved("card")), with(MatchedItem.TEXT), with(TargetZoneMatcher.path("selector-tbd")), with(false), with("hello"));
		}});
		TestStepParser utp = new TestStepParser(tracker, namer, builder, topLevel);
		TDAParsing nested = utp.tryParsing(UnitTestTopLevelParsingTests.line("match card text selector-tbd"));
		assertTrue(nested instanceof FreeTextParser);
		nested.tryParsing(UnitTestTopLevelParsingTests.line("hello"));
		nested.scopeComplete(pos);
		utp.scopeComplete(pos);
	}
	
	@Test
	public void testWeCanMatchAPortionOfATextNode() {
		context.checking(new Expectations() {{
			oneOf(builder).match((UnresolvedVar)with(ExprMatcher.unresolved("card")), with(MatchedItem.TEXT), with(TargetZoneMatcher.path()), with(true), with("hello"));
		}});
		TestStepParser utp = new TestStepParser(tracker, namer, builder, topLevel);
		TDAParsing nested = utp.tryParsing(UnitTestTopLevelParsingTests.line("match card text contains"));
		assertTrue(nested instanceof FreeTextParser);
		nested.tryParsing(UnitTestTopLevelParsingTests.line("hello"));
		nested.scopeComplete(pos);
		utp.scopeComplete(pos);
	}
	
	@Test
	public void testWeCanMatchAPortionOfTheTextSelectedWithASelector() {
		context.checking(new Expectations() {{
			oneOf(builder).match((UnresolvedVar)with(ExprMatcher.unresolved("card")), with(MatchedItem.TEXT), with(TargetZoneMatcher.path("selector-tbd")), with(true), with("hello"));
		}});
		TestStepParser utp = new TestStepParser(tracker, namer, builder, topLevel);
		TDAParsing nested = utp.tryParsing(UnitTestTopLevelParsingTests.line("match card text selector-tbd contains"));
		assertTrue(nested instanceof FreeTextParser);
		nested.tryParsing(UnitTestTopLevelParsingTests.line("hello"));
		nested.scopeComplete(pos);
		utp.scopeComplete(pos);
	}
	
	@Test
	public void testWeCanHandleADataDefinitionStep() {
		context.checking(new Expectations() {{
			oneOf(namer).dataName(with(any(InputPosition.class)), with("x")); will(returnValue(FunctionName.function(pos, pkg, "x")));
			oneOf(builder).data(with(any(UnitDataDeclaration.class)));
			oneOf(topLevel).nestedData(with(any(UnitDataDeclaration.class)));
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
	
	
	@Test
	public void testErrorsTurnUpInTheRightPlace() {
		final Tokenizable line = UnitTestTopLevelParsingTests.line("assert (f ['hello', world'])");
		context.checking(new Expectations() {{
			oneOf(errors).message(new InputPosition("fred", 1, 25, ""), "unterminated string");
		}});
		TestStepParser utp = new TestStepParser(tracker, namer, builder, topLevel);
		TDAParsing nested = utp.tryParsing(line);
		assertTrue(nested instanceof IgnoreNestedParser);
		nested.scopeComplete(pos);
		utp.scopeComplete(pos);
	}

}
