package test.parsing;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.util.function.Consumer;

import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.commonBase.Pattern;
import org.flasck.flas.commonBase.names.PackageName;
import org.flasck.flas.commonBase.names.VarName;
import org.flasck.flas.errors.ErrorReporter;
import org.flasck.flas.errors.LocalErrorTracker;
import org.flasck.flas.grammar.tracking.LoggableToken;
import org.flasck.flas.parsedForm.TypedPattern;
import org.flasck.flas.parsedForm.VarPattern;
import org.flasck.flas.parser.FunctionScopeUnitConsumer;
import org.flasck.flas.parser.TDAParsing;
import org.flasck.flas.parser.TDAPatternParser;
import org.flasck.flas.parser.VarNamer;
import org.flasck.flas.tokenizers.Tokenizable;
import org.jmock.Expectations;
import org.jmock.integration.junit4.JUnitRuleMockery;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.zinutils.support.jmock.ReturnInvoker;

import flas.matchers.ConstPatternMatcher;
import flas.matchers.CtorPatternMatcher;
import flas.matchers.PatternMatcher;
import flas.matchers.TuplePatternMatcher;
import flas.matchers.TypedPatternMatcher;
import flas.matchers.VarPatternMatcher;
import test.flas.stories.TDAStoryTests;

public class TDAPatternParsingTests {
	@Rule public JUnitRuleMockery context = new JUnitRuleMockery();
	private InputPosition pos = new InputPosition("-", 1, 0, null, "hello");
	private ErrorReporter errorsMock = context.mock(ErrorReporter.class);
	private ErrorReporter errors = new LocalErrorTracker(errorsMock);
	@SuppressWarnings("unchecked")
	private Consumer<Pattern> builder = context.mock(Consumer.class);
	private VarNamer vnamer = context.mock(VarNamer.class);
	private FunctionScopeUnitConsumer topLevel = context.mock(FunctionScopeUnitConsumer.class);
	private PackageName pkg = new PackageName("test.pkg");

	@Before
	public void ignoreParserLogging() {
		context.checking(new Expectations() {{
			allowing(errorsMock).logParsingToken(with(any(LoggableToken.class))); will(ReturnInvoker.arg(0));
		}});
	}

	@Test
	public void atTheEndOfTheLineReturnNull() {
		final Tokenizable line = line("");
		context.checking(new Expectations() {{
		}});
		TDAPatternParser parser = new TDAPatternParser(errors, vnamer, builder, topLevel);
		TDAParsing nested = parser.tryParsing(line);
		assertNull(nested);
	}

	@Test
	public void ifYouEncounterEqualsThenThatsNotAPattern() { // I'm particularly interested in this case because it happens with functions a lot
		final Tokenizable line = line("=");
		context.checking(new Expectations() {{
		}});
		TDAPatternParser parser = new TDAPatternParser(errors, vnamer, builder, topLevel);
		TDAParsing nested = parser.tryParsing(line);
		assertNull(nested);
		assertEquals(0, line.at());
	}

	@Test
	public void numbersCanBePatterns() {
		final Tokenizable line = line("42");
		context.checking(new Expectations() {{
			oneOf(builder).accept(with(ConstPatternMatcher.number(42)));
		}});
		TDAPatternParser parser = new TDAPatternParser(errors, vnamer, builder, topLevel);
		TDAParsing canContinue = parser.tryParsing(line);
		assertNotNull(canContinue);
		assertNull(parser.tryParsing(line));
	}

	@Test
	public void stringsCanBePatterns() {
		final Tokenizable line = line("'hello'");
		context.checking(new Expectations() {{
			oneOf(builder).accept(with(ConstPatternMatcher.string("hello")));
		}});
		TDAPatternParser parser = new TDAPatternParser(errors, vnamer, builder, topLevel);
		TDAParsing canContinue = parser.tryParsing(line);
		assertNotNull(canContinue);
		assertNull(parser.tryParsing(line));
	}

	@Test
	public void numbersCanBePatternsInsideParens() { // except: why?
		final Tokenizable line = line("(42)");
		context.checking(new Expectations() {{
			oneOf(builder).accept(with(ConstPatternMatcher.number(42)));
		}});
		TDAPatternParser parser = new TDAPatternParser(errors, vnamer, builder, topLevel);
		TDAParsing canContinue = parser.tryParsing(line);
		assertNotNull(canContinue);
		assertNull(parser.tryParsing(line));
	}

	@Test
	public void trueCanBeAPattern() {
		final Tokenizable line = line("true");
		context.checking(new Expectations() {{
			oneOf(builder).accept(with(ConstPatternMatcher.truth(true)));
		}});
		TDAPatternParser parser = new TDAPatternParser(errors, vnamer, builder, topLevel);
		TDAParsing canContinue = parser.tryParsing(line);
		assertNotNull(canContinue);
		assertNull(parser.tryParsing(line));
	}

	@Test
	public void falseCanBeAPattern() {
		final Tokenizable line = line("false");
		context.checking(new Expectations() {{
			oneOf(builder).accept(with(ConstPatternMatcher.truth(false)));
		}});
		TDAPatternParser parser = new TDAPatternParser(errors, vnamer, builder, topLevel);
		TDAParsing canContinue = parser.tryParsing(line);
		assertNotNull(canContinue);
		assertNull(parser.tryParsing(line));
	}

	@Test
	public void aVariableIsAPatternByItselfAndAllowsYouToContinue() {
		final Tokenizable line = line("x");
		context.checking(new Expectations() {{
			oneOf(vnamer).nameVar(with(any(InputPosition.class)), with("x")); will(returnValue(new VarName(line.realinfo(), pkg, "x")));
			oneOf(builder).accept(with(VarPatternMatcher.var("test.pkg.x")));
			oneOf(topLevel).argument(with(aNull(ErrorReporter.class)), with(any(VarPattern.class)));
		}});
		TDAPatternParser parser = new TDAPatternParser(errors, vnamer, builder, topLevel);
		TDAParsing canContinue = parser.tryParsing(line);
		assertNotNull(canContinue);
		assertNull(parser.tryParsing(line));
	}

	@Test
	public void twoVariablesCanBeFoundOnTheSameLine() {
		final Tokenizable line = line("x y");
		context.checking(new Expectations() {{
			oneOf(vnamer).nameVar(with(any(InputPosition.class)), with("x")); will(returnValue(new VarName(line.realinfo(), pkg, "x")));
			oneOf(builder).accept(with(VarPatternMatcher.var("test.pkg.x")));
			oneOf(topLevel).argument(with(aNull(ErrorReporter.class)), with(any(VarPattern.class)));
			oneOf(vnamer).nameVar(with(any(InputPosition.class)), with("y")); will(returnValue(new VarName(line.realinfo(), pkg, "y")));
			oneOf(builder).accept(with(VarPatternMatcher.var("test.pkg.y")));
			oneOf(topLevel).argument(with(aNull(ErrorReporter.class)), with(any(VarPattern.class)));
		}});
		TDAPatternParser parser = new TDAPatternParser(errors, vnamer, builder, topLevel);
		TDAParsing canContinue = parser.tryParsing(line);
		assertNotNull(canContinue);
		TDAParsing canContinue2 = parser.tryParsing(line);
		assertNotNull(canContinue2);
		assertNull(parser.tryParsing(line));
	}

	@Test
	public void anOpenParenIsASyntaxError() {
		final Tokenizable line = line("(");
		context.checking(new Expectations() {{
			oneOf(errorsMock).message(line, "invalid pattern");
		}});
		TDAPatternParser parser = new TDAPatternParser(errors, vnamer, builder, topLevel);
		TDAParsing canContinue = parser.tryParsing(line);
		assertNull(canContinue);
	}

	@Test
	public void openAndCloseIsASyntaxError() {
		final Tokenizable line = line("()");
		context.checking(new Expectations() {{
			oneOf(errorsMock).message(line, "invalid pattern");
		}});
		TDAPatternParser parser = new TDAPatternParser(errors, vnamer, builder, topLevel);
		TDAParsing canContinue = parser.tryParsing(line);
		assertNull(canContinue);
	}

	@Test
	public void aVarInParensIsJustAVar() {
		final Tokenizable line = line("(x)");
		context.checking(new Expectations() {{
			oneOf(vnamer).nameVar(with(any(InputPosition.class)), with("x")); will(returnValue(new VarName(line.realinfo(), pkg, "x")));
			oneOf(builder).accept(with(VarPatternMatcher.var("test.pkg.x")));
			oneOf(topLevel).argument(with(aNull(ErrorReporter.class)), with(any(VarPattern.class)));
		}});
		TDAPatternParser parser = new TDAPatternParser(errors, vnamer, builder, topLevel);
		TDAParsing canContinue = parser.tryParsing(line);
		assertNotNull(canContinue);
	}

	@Test
	public void anUnclosedVarIsStillASyntaxErrorEvenThoughWeReportThePresenceOfTheVar() {
		final Tokenizable line = line("(x");
		context.checking(new Expectations() {{
			oneOf(vnamer).nameVar(with(any(InputPosition.class)), with("x")); will(returnValue(new VarName(line.realinfo(), pkg, "x")));
			oneOf(topLevel).argument(with(aNull(ErrorReporter.class)), with(any(VarPattern.class)));
			oneOf(errorsMock).message(line, "invalid pattern");
		}});
		TDAPatternParser parser = new TDAPatternParser(errors, vnamer, builder, topLevel);
		TDAParsing canContinue = parser.tryParsing(line);
		assertNull(canContinue);
	}

	@Test
	public void aConstructorByItselfIsJustATypeWithNoArgs() {
		final Tokenizable line = line("Nil");
		context.checking(new Expectations() {{
			oneOf(builder).accept(with(CtorPatternMatcher.ctor("Nil")));
		}});
		TDAPatternParser parser = new TDAPatternParser(errors, vnamer, builder, topLevel);
		TDAParsing canContinue = parser.tryParsing(line);
		assertNotNull(canContinue);
		assertNull(parser.tryParsing(line));
	}

	@Test
	public void anUnParenedConstructorDoesNotSwallowTheRemainingVars() {
		final Tokenizable line = line("Nil x");
		context.checking(new Expectations() {{
			oneOf(vnamer).nameVar(with(any(InputPosition.class)), with("x")); will(returnValue(new VarName(line.realinfo(), pkg, "x")));
			oneOf(builder).accept(with(CtorPatternMatcher.ctor("Nil")));
			oneOf(builder).accept(with(VarPatternMatcher.var("test.pkg.x")));
			oneOf(topLevel).argument(with(aNull(ErrorReporter.class)), with(any(VarPattern.class)));
		}});
		TDAPatternParser parser = new TDAPatternParser(errors, vnamer, builder, topLevel);
		TDAParsing canContinue = parser.tryParsing(line);
		assertNotNull(canContinue);
		canContinue = parser.tryParsing(line);
		assertNotNull(canContinue);
		assertNull(parser.tryParsing(line));
	}

	@Test
	public void aConstructorByItselfCanBePlacedInParensIfYouWant() {
		final Tokenizable line = line("(Nil)");
		context.checking(new Expectations() {{
			oneOf(builder).accept(with(CtorPatternMatcher.ctor("Nil")));
		}});
		TDAPatternParser parser = new TDAPatternParser(errors, vnamer, builder, topLevel);
		TDAParsing canContinue = parser.tryParsing(line);
		assertNotNull(canContinue);
		assertNull(parser.tryParsing(line));
	}

	@Test
	public void parensCanContainTypedThings() {
		final Tokenizable line = line("(String x)");
		context.checking(new Expectations() {{
			oneOf(vnamer).nameVar(with(any(InputPosition.class)), with("x")); will(returnValue(new VarName(pos, null, "x")));
			oneOf(builder).accept(with(TypedPatternMatcher.typed("String", "x")));
			oneOf(topLevel).argument(with(errors), with(any(TypedPattern.class)));
		}});
		TDAPatternParser parser = new TDAPatternParser(errors, vnamer, builder, topLevel);
		TDAParsing canContinue = parser.tryParsing(line);
		assertNotNull(canContinue);
		assertNull(parser.tryParsing(line));
	}

	@Test
	public void parensCanContainPolyTypedThings() {
		final Tokenizable line = line("(A x)");
		context.checking(new Expectations() {{
//			oneOf(vnamer).namePoly(with(any(InputPosition.class)), with("A")); will(returnValue(new SolidName(null, "A")));
//			oneOf(topLevel).polytype(with(errors), with(any(PolyType.class)));
			oneOf(vnamer).nameVar(with(any(InputPosition.class)), with("x")); will(returnValue(new VarName(pos, null, "x")));
			oneOf(builder).accept(with(TypedPatternMatcher.typed("A", "x")));
			oneOf(topLevel).argument(with(errors), with(any(TypedPattern.class)));
		}});
		TDAPatternParser parser = new TDAPatternParser(errors, vnamer, builder, topLevel);
		TDAParsing canContinue = parser.tryParsing(line);
		assertNotNull(canContinue);
		assertNull(parser.tryParsing(line));
	}

	@Test
	public void itIsStillAnErrorNotToCloseYourParens() {
		final Tokenizable line = line("(String x");
		context.checking(new Expectations() {{
			oneOf(vnamer).nameVar(with(any(InputPosition.class)), with("x")); will(returnValue(new VarName(pos, null, "x")));
			oneOf(topLevel).argument(with(errors), with(any(TypedPattern.class)));
			oneOf(errorsMock).message(line, "invalid pattern");
		}});
		TDAPatternParser parser = new TDAPatternParser(errors, vnamer, builder, topLevel);
		TDAParsing canContinue = parser.tryParsing(line);
		assertNull(canContinue);
	}

	@Test
	public void trivialConstructorMatchSyntaxWorks() {
		final Tokenizable line = line("(Nil {})");
		context.checking(new Expectations() {{
			oneOf(builder).accept(with(CtorPatternMatcher.ctor("Nil")));
		}});
		TDAPatternParser parser = new TDAPatternParser(errors, vnamer, builder, topLevel);
		TDAParsing canContinue = parser.tryParsing(line);
		assertNotNull(canContinue);
		assertNull(parser.tryParsing(line));
	}

	@Test
	public void canPutRandomCharsAtTheEnd() {
		final Tokenizable line = line("(Nil:");
		context.checking(new Expectations() {{
			oneOf(errorsMock).message(line, "invalid pattern");
		}});
		TDAPatternParser parser = new TDAPatternParser(errors, vnamer, builder, topLevel);
		TDAParsing canContinue = parser.tryParsing(line);
		assertNull(canContinue);
	}

	@Test
	public void trivialConstructorMatchRequiresCCB() {
		final Tokenizable line = line("(Nil {");
		context.checking(new Expectations() {{
			oneOf(errorsMock).message(line, "invalid pattern");
		}});
		TDAPatternParser parser = new TDAPatternParser(errors, vnamer, builder, topLevel);
		TDAParsing canContinue = parser.tryParsing(line);
		assertNull(canContinue);
	}

	@Test
	public void trivialConstructorMatchRequiresCRBAfterPattern() {
		final Tokenizable line = line("(Nil {}");
		context.checking(new Expectations() {{
			oneOf(errorsMock).message(line, "invalid pattern");
		}});
		TDAPatternParser parser = new TDAPatternParser(errors, vnamer, builder, topLevel);
		TDAParsing canContinue = parser.tryParsing(line);
		assertNull(canContinue);
	}

	@Test
	public void aConstructorCanHaveAParameter() {
		final Tokenizable line = line("(Cons {tail: Nil})");
		context.checking(new Expectations() {{
			oneOf(builder).accept(with(CtorPatternMatcher.ctor("Cons").field("tail", CtorPatternMatcher.ctor("Nil"))));
		}});
		TDAPatternParser parser = new TDAPatternParser(errors, vnamer, builder, topLevel);
		TDAParsing canContinue = parser.tryParsing(line);
		assertNotNull(canContinue);
		assertNull(parser.tryParsing(line));
	}

	@Test
	public void aParameterCannotJustEnd() {
		final Tokenizable line = line("(Cons {");
		context.checking(new Expectations() {{
			oneOf(errorsMock).message(line, "invalid pattern");
		}});
		TDAPatternParser parser = new TDAPatternParser(errors, vnamer, builder, topLevel);
		TDAParsing canContinue = parser.tryParsing(line);
		assertNull(canContinue);
	}

	@Test
	public void theFirstThingInAParameterCannotBeAColon() {
		final Tokenizable line = line("(Cons {: Nil})");
		context.checking(new Expectations() {{
			oneOf(errorsMock).message(line, "invalid pattern");
		}});
		TDAPatternParser parser = new TDAPatternParser(errors, vnamer, builder, topLevel);
		TDAParsing canContinue = parser.tryParsing(line);
		assertNull(canContinue);
	}

	@Test
	public void theParameterCannotHaveATypeForAFieldName() {
		final Tokenizable line = line("(Cons {Nil: Nil})");
		context.checking(new Expectations() {{
			oneOf(errorsMock).message(line, "invalid pattern");
		}});
		TDAPatternParser parser = new TDAPatternParser(errors, vnamer, builder, topLevel);
		TDAParsing canContinue = parser.tryParsing(line);
		assertNull(canContinue);
	}

	@Test
	public void theParameterMustHaveAPattern() {
		final Tokenizable line = line("(Cons {tail: })");
		context.checking(new Expectations() {{
			oneOf(errorsMock).message(line, "invalid pattern");
		}});
		TDAPatternParser parser = new TDAPatternParser(errors, vnamer, builder, topLevel);
		TDAParsing canContinue = parser.tryParsing(line);
		assertNull(canContinue);
	}

	@Test
	public void aParameterCanHaveANestedPattern() {
		final Tokenizable line = line("(Cons { tail: (Nil) })");
		context.checking(new Expectations() {{
			oneOf(builder).accept(with(CtorPatternMatcher.ctor("Cons").field("tail", CtorPatternMatcher.ctor("Nil"))));
		}});
		TDAPatternParser parser = new TDAPatternParser(errors, vnamer, builder, topLevel);
		TDAParsing canContinue = parser.tryParsing(line);
		assertNotNull(canContinue);
		assertNull(parser.tryParsing(line));
	}

	@Test
	public void aConstructorCanHaveMultiplePatterns() {
		final Tokenizable line = line("(Cons { head: 0, tail: (Nil) })");
		context.checking(new Expectations() {{
			oneOf(builder).accept(with(CtorPatternMatcher.ctor("Cons").field("head", ConstPatternMatcher.number(0)).field("tail", CtorPatternMatcher.ctor("Nil"))));
		}});
		TDAPatternParser parser = new TDAPatternParser(errors, vnamer, builder, topLevel);
		TDAParsing canContinue = parser.tryParsing(line);
		assertNotNull(canContinue);
		assertNull(parser.tryParsing(line));
	}

	// To be fair, this isn't really an "intended" behavior, it's just that the price of having it the other
	// way much exceeded its value, so I went with the more flexible approach.
	// This test is just here to document that.
	@Test
	public void aPatternCanEndWithAComma() {
		final Tokenizable line = line("(Cons { head: 0, })");
		context.checking(new Expectations() {{
			oneOf(builder).accept(with(CtorPatternMatcher.ctor("Cons").field("head", ConstPatternMatcher.number(0))));
		}});
		TDAPatternParser parser = new TDAPatternParser(errors, vnamer, builder, topLevel);
		TDAParsing canContinue = parser.tryParsing(line);
		assertNotNull(canContinue);
		assertNull(parser.tryParsing(line));
	}

	@Test
	public void aConstructorCanHaveAQualifiedName() {
		final Tokenizable line = line("basic.Nil");
		context.checking(new Expectations() {{
			oneOf(builder).accept(with(CtorPatternMatcher.ctor("basic.Nil"))); // TODO: should we break this up?
		}});
		TDAPatternParser parser = new TDAPatternParser(errors, vnamer, builder, topLevel);
		TDAParsing canContinue = parser.tryParsing(line);
		assertNotNull(canContinue);
		assertNull(parser.tryParsing(line));
		assertFalse(line.hasMore());
	}

	@Test
	public void aTypeCanHaveParameters() {
		final Tokenizable line = line("(Cons[Number] nl)");
		context.checking(new Expectations() {{
			oneOf(vnamer).nameVar(with(any(InputPosition.class)), with("nl")); will(returnValue(new VarName(pos, null, "nl")));
			oneOf(builder).accept(with(TypedPatternMatcher.typed("Cons", "nl").typevar("Number")));
			oneOf(topLevel).argument(with(errors), with(any(TypedPattern.class)));
		}});
		TDAPatternParser parser = new TDAPatternParser(errors, vnamer, builder, topLevel);
		TDAParsing canContinue = parser.tryParsing(line);
		assertNotNull(canContinue);
		assertNull(parser.tryParsing(line));
		assertFalse(line.hasMore());
	}

	@Test
	public void aTypeCanHaveParametersWithParameters() {
		final Tokenizable line = line("(Map[A,List[A]] map)");
		context.checking(new Expectations() {{
//			exactly(2).of(vnamer).namePoly(with(any(InputPosition.class)), with("A")); will(returnValue(new SolidName(null, "A")));
//			exactly(2).of(topLevel).polytype(with(errors), with(any(PolyType.class)));
			oneOf(vnamer).nameVar(with(any(InputPosition.class)), with("map")); will(returnValue(new VarName(pos, null, "map")));
			oneOf(builder).accept(with(TypedPatternMatcher.typed("Map", "map").typevar("A").typevar("List")));
			oneOf(topLevel).argument(with(errors), with(any(TypedPattern.class)));
		}});
		TDAPatternParser parser = new TDAPatternParser(errors, vnamer, builder, topLevel);
		TDAParsing canContinue = parser.tryParsing(line);
		assertNotNull(canContinue);
		assertNull(parser.tryParsing(line));
		assertFalse(line.hasMore());
	}

	@Test
	public void aTypeWithParametersMustIntroduceAVar() {
		final Tokenizable line = line("(Cons[Number])");
		context.checking(new Expectations() {{
			oneOf(errorsMock).message(line, "type parameters can only be used with type patterns");
		}});
		TDAPatternParser parser = new TDAPatternParser(errors, vnamer, builder, topLevel);
		TDAParsing canContinue = parser.tryParsing(line);
		assertNull(canContinue);
	}

	@Test
	public void aTypeWithParametersMustBeInParensOtherwiseYouGetTwoArgumentsTheFirstBeingATypeConstantAndTheSecondOfWhichIsAList() {
		final Tokenizable line = line("Type[A]");
		context.checking(new Expectations() {{
			oneOf(builder).accept(with(CtorPatternMatcher.ctor("Type")));
			oneOf(builder).accept(with(CtorPatternMatcher.ctor("Cons").field("head", TypedPatternMatcher.ctor("A")).field("tail", TypedPatternMatcher.ctor("Nil"))));
		}});
		TDAPatternParser parser = new TDAPatternParser(errors, vnamer, builder, topLevel);
		TDAParsing canContinue = parser.tryParsing(line);
		assertNotNull(canContinue);
		assertNotNull(parser.tryParsing(line));
	}

	@Test
	public void aTypeWithParametersMustBeInParensAndHaveAVarToGetTheRightResult() {
		final Tokenizable line = line("(Type[A] var)");
		context.checking(new Expectations() {{
//			oneOf(vnamer).namePoly(with(any(InputPosition.class)), with("A")); will(returnValue(new SolidName(null, "A")));
//			oneOf(topLevel).polytype(with(errors), with(any(PolyType.class)));
			oneOf(vnamer).nameVar(with(any(InputPosition.class)), with("var")); will(returnValue(new VarName(pos, null, "var")));
			oneOf(builder).accept(with(TypedPatternMatcher.typed("Type", "var").typevar("A")));
			oneOf(topLevel).argument(with(errors), with(any(TypedPattern.class)));
		}});
		TDAPatternParser parser = new TDAPatternParser(errors, vnamer, builder, topLevel);
		TDAParsing canContinue = parser.tryParsing(line);
		assertNotNull(canContinue);
		assertNull(parser.tryParsing(line));
	}

	@Test
	public void aTypeWithParametersCannotIntroduceAConstructorMatch() {
		final Tokenizable line = line("(Cons[Number] {})");
		context.checking(new Expectations() {{
			oneOf(errorsMock).message(line, "type parameters can only be used with type patterns");
		}});
		TDAPatternParser parser = new TDAPatternParser(errors, vnamer, builder, topLevel);
		TDAParsing canContinue = parser.tryParsing(line);
		assertNull(canContinue);
	}

	@Test
	public void aTypeCanHaveMultipleParameters() {
		final Tokenizable line = line("(Map[Number,String] map)");
		context.checking(new Expectations() {{
			oneOf(vnamer).nameVar(with(any(InputPosition.class)), with("map")); will(returnValue(new VarName(pos, null, "map")));
			oneOf(builder).accept(with(TypedPatternMatcher.typed("Map", "map").typevar("Number").typevar("String")));
			oneOf(topLevel).argument(with(errors), with(any(TypedPattern.class)));
		}});
		TDAPatternParser parser = new TDAPatternParser(errors, vnamer, builder, topLevel);
		TDAParsing canContinue = parser.tryParsing(line);
		assertNotNull(canContinue);
		assertNull(parser.tryParsing(line));
	}

	@Test
	public void aTypeMustHaveACommaBetweenParameters() {
		final Tokenizable line = line("(Map[Number String] map)");
		context.checking(new Expectations() {{
			oneOf(errorsMock).message(line, "invalid pattern");
		}});
		TDAPatternParser parser = new TDAPatternParser(errors, vnamer, builder, topLevel);
		TDAParsing canContinue = parser.tryParsing(line);
		assertNull(canContinue);
	}

	@Test
	public void aTypeParameterCannotSuddenlyLaunchIntoSyntax() {
		final Tokenizable line = line("(Map[Number{}] map)");
		context.checking(new Expectations() {{
			oneOf(errorsMock).message(line, "invalid pattern");
		}});
		TDAPatternParser parser = new TDAPatternParser(errors, vnamer, builder, topLevel);
		TDAParsing canContinue = parser.tryParsing(line);
		assertNull(canContinue);
	}

	@Test
	public void aTypeParameterMustBeClosed() {
		final Tokenizable line = line("(Map[Number) map)");
		context.checking(new Expectations() {{
			oneOf(errorsMock).message(line, "invalid pattern");
		}});
		TDAPatternParser parser = new TDAPatternParser(errors, vnamer, builder, topLevel);
		TDAParsing canContinue = parser.tryParsing(line);
		assertNull(canContinue);
	}

	@Test
	public void aTypeParameterMustBeClosedBeforeVar() {
		final Tokenizable line = line("(Map[Number map)");
		context.checking(new Expectations() {{
			oneOf(errorsMock).message(line, "invalid pattern");
		}});
		TDAPatternParser parser = new TDAPatternParser(errors, vnamer, builder, topLevel);
		TDAParsing canContinue = parser.tryParsing(line);
		assertNull(canContinue);
	}

	@Test
	public void listsAreHandledAsASpecialCase() {
		final Tokenizable line = line("[]");
		context.checking(new Expectations() {{
			oneOf(builder).accept(with(CtorPatternMatcher.ctor("Nil")));
		}});
		TDAPatternParser parser = new TDAPatternParser(errors, vnamer, builder, topLevel);
		TDAParsing canContinue = parser.tryParsing(line);
		assertNotNull(canContinue);
		assertNull(parser.tryParsing(line));
	}

	@Test
	public void aListCanNestAPatternForTheHead() {
		final Tokenizable line = line("[42]");
		context.checking(new Expectations() {{
			oneOf(builder).accept(with(CtorPatternMatcher.ctor("Cons").field("head", ConstPatternMatcher.number(42)).field("tail", CtorPatternMatcher.ctor("Nil"))));
		}});
		TDAPatternParser parser = new TDAPatternParser(errors, vnamer, builder, topLevel);
		TDAParsing canContinue = parser.tryParsing(line);
		assertNotNull(canContinue);
		assertNull(parser.tryParsing(line));
	}

	@Test
	public void aListCanNestAVarPatternForTheHead() {
		final Tokenizable line = line("[a]");
		context.checking(new Expectations() {{
			oneOf(vnamer).nameVar(with(any(InputPosition.class)), with("a")); will(returnValue(new VarName(line.realinfo(), pkg, "a")));
			oneOf(builder).accept(with(CtorPatternMatcher.ctor("Cons").field("head", PatternMatcher.var("test.pkg.a")).field("tail", CtorPatternMatcher.ctor("Nil"))));
			oneOf(topLevel).argument(with(aNull(ErrorReporter.class)), with(any(VarPattern.class)));
		}});
		TDAPatternParser parser = new TDAPatternParser(errors, vnamer, builder, topLevel);
		TDAParsing canContinue = parser.tryParsing(line);
		assertNotNull(canContinue);
		assertNull(parser.tryParsing(line));
	}

	@Test
	public void aListCanNestTwoPatterns() {
		final Tokenizable line = line("[42, 86]");
		context.checking(new Expectations() {{
			oneOf(builder).accept(with(CtorPatternMatcher.ctor("Cons").field("head", ConstPatternMatcher.number(42)).field("tail", CtorPatternMatcher.ctor("Cons").field("head", ConstPatternMatcher.number(86)).field("tail", CtorPatternMatcher.ctor("Nil")))));
		}});
		TDAPatternParser parser = new TDAPatternParser(errors, vnamer, builder, topLevel);
		TDAParsing canContinue = parser.tryParsing(line);
		assertNotNull(canContinue);
		assertNull(parser.tryParsing(line));
	}

	@Test
	public void aPatternCanBeATuple() {
		final Tokenizable line = line("(42, 86)");
		context.checking(new Expectations() {{
			oneOf(builder).accept(with(TuplePatternMatcher.tuple().member(ConstPatternMatcher.number(42)).member(ConstPatternMatcher.number(86))));
		}});
		TDAPatternParser parser = new TDAPatternParser(errors, vnamer, builder, topLevel);
		TDAParsing canContinue = parser.tryParsing(line);
		assertNotNull(canContinue);
		assertNull(parser.tryParsing(line));
	}
	
	public static Tokenizable line(String string) {
		return new Tokenizable(TDAStoryTests.line(string));
	}
}
