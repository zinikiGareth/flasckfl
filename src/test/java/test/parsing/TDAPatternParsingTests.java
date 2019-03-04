package test.parsing;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.util.function.Consumer;

import org.flasck.flas.commonBase.Pattern;
import org.flasck.flas.errors.ErrorReporter;
import org.flasck.flas.parser.LocalErrorTracker;
import org.flasck.flas.parser.TDAParsing;
import org.flasck.flas.parser.TDAPatternParser;
import org.flasck.flas.tokenizers.Tokenizable;
import org.jmock.Expectations;
import org.jmock.integration.junit4.JUnitRuleMockery;
import org.junit.Rule;
import org.junit.Test;

import test.flas.stories.TDAStoryTests;

public class TDAPatternParsingTests {
	@Rule public JUnitRuleMockery context = new JUnitRuleMockery();
	private ErrorReporter errorsMock = context.mock(ErrorReporter.class);
	private ErrorReporter errors = new LocalErrorTracker(errorsMock);
	@SuppressWarnings("unchecked")
	private Consumer<Pattern> builder = context.mock(Consumer.class);

	@Test
	public void atTheEndOfTheLineReturnNull() {
		final Tokenizable line = line("");
		context.checking(new Expectations() {{
		}});
		TDAPatternParser parser = new TDAPatternParser(errors, builder);
		TDAParsing nested = parser.tryParsing(line);
		assertNull(nested);
	}

	@Test
	public void ifYouEncounterEqualsThenThatsNotAPattern() { // I'm particularly interested in this case because it happens with functions a lot
		final Tokenizable line = line("=");
		context.checking(new Expectations() {{
		}});
		TDAPatternParser parser = new TDAPatternParser(errors, builder);
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
		TDAPatternParser parser = new TDAPatternParser(errors, builder);
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
		TDAPatternParser parser = new TDAPatternParser(errors, builder);
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
		TDAPatternParser parser = new TDAPatternParser(errors, builder);
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
		TDAPatternParser parser = new TDAPatternParser(errors, builder);
		TDAParsing canContinue = parser.tryParsing(line);
		assertNotNull(canContinue);
		assertNull(parser.tryParsing(line));
	}

	@Test
	public void aVariableIsAPatternByItselfAndAllowsYouToContinue() {
		final Tokenizable line = line("x");
		context.checking(new Expectations() {{
			oneOf(builder).accept(with(VarPatternMatcher.var("x")));
		}});
		TDAPatternParser parser = new TDAPatternParser(errors, builder);
		TDAParsing canContinue = parser.tryParsing(line);
		assertNotNull(canContinue);
		assertNull(parser.tryParsing(line));
	}

	@Test
	public void twoVariablesCanBeFoundOnTheSameLine() {
		final Tokenizable line = line("x y");
		context.checking(new Expectations() {{
			oneOf(builder).accept(with(VarPatternMatcher.var("x")));
			oneOf(builder).accept(with(VarPatternMatcher.var("y")));
		}});
		TDAPatternParser parser = new TDAPatternParser(errors, builder);
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
		TDAPatternParser parser = new TDAPatternParser(errors, builder);
		TDAParsing canContinue = parser.tryParsing(line);
		assertNull(canContinue);
	}

	@Test
	public void openAndCloseIsASyntaxError() {
		final Tokenizable line = line("()");
		context.checking(new Expectations() {{
			oneOf(errorsMock).message(line, "invalid pattern");
		}});
		TDAPatternParser parser = new TDAPatternParser(errors, builder);
		TDAParsing canContinue = parser.tryParsing(line);
		assertNull(canContinue);
	}

	@Test
	public void aVarInParensIsJustAVar() {
		final Tokenizable line = line("(x)");
		context.checking(new Expectations() {{
			oneOf(builder).accept(with(VarPatternMatcher.var("x")));
		}});
		TDAPatternParser parser = new TDAPatternParser(errors, builder);
		TDAParsing canContinue = parser.tryParsing(line);
		assertNotNull(canContinue);
	}

	@Test
	public void anUnclosedVarIsStillASyntaxErrorEvenThoughWeReportThePresenceOfTheVar() {
		final Tokenizable line = line("(x");
		context.checking(new Expectations() {{
			oneOf(builder).accept(with(VarPatternMatcher.var("x")));
			oneOf(errorsMock).message(line, "invalid pattern");
		}});
		TDAPatternParser parser = new TDAPatternParser(errors, builder);
		TDAParsing canContinue = parser.tryParsing(line);
		assertNull(canContinue);
	}

	@Test
	public void aConstructorByItselfIsJustATypeWithNoArgs() {
		final Tokenizable line = line("Nil");
		context.checking(new Expectations() {{
			oneOf(builder).accept(with(CtorPatternMatcher.ctor("Nil")));
		}});
		TDAPatternParser parser = new TDAPatternParser(errors, builder);
		TDAParsing canContinue = parser.tryParsing(line);
		assertNotNull(canContinue);
		assertNull(parser.tryParsing(line));
	}

	@Test
	public void anUnParenedConstructorDoesNotSwallowTheRemainingVars() {
		final Tokenizable line = line("Nil x");
		context.checking(new Expectations() {{
			oneOf(builder).accept(with(CtorPatternMatcher.ctor("Nil")));
			oneOf(builder).accept(with(VarPatternMatcher.var("x")));
		}});
		TDAPatternParser parser = new TDAPatternParser(errors, builder);
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
		TDAPatternParser parser = new TDAPatternParser(errors, builder);
		TDAParsing canContinue = parser.tryParsing(line);
		assertNotNull(canContinue);
		assertNull(parser.tryParsing(line));
	}

	@Test
	public void parensCanContainTypedThings() {
		final Tokenizable line = line("(String x)");
		context.checking(new Expectations() {{
			oneOf(builder).accept(with(TypedPatternMatcher.typed("String", "x")));
		}});
		TDAPatternParser parser = new TDAPatternParser(errors, builder);
		TDAParsing canContinue = parser.tryParsing(line);
		assertNotNull(canContinue);
		assertNull(parser.tryParsing(line));
	}

	@Test
	public void itIsStillAnErrorNotToCloseYourParens() {
		final Tokenizable line = line("(String x");
		context.checking(new Expectations() {{
			oneOf(builder).accept(with(TypedPatternMatcher.typed("String", "x")));
			oneOf(errorsMock).message(line, "invalid pattern");
		}});
		TDAPatternParser parser = new TDAPatternParser(errors, builder);
		TDAParsing canContinue = parser.tryParsing(line);
		assertNull(canContinue);
	}

	@Test
	public void trivialConstructorMatchSyntaxWorks() {
		final Tokenizable line = line("(Nil {})");
		context.checking(new Expectations() {{
			oneOf(builder).accept(with(CtorPatternMatcher.ctor("Nil")));
		}});
		TDAPatternParser parser = new TDAPatternParser(errors, builder);
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
		TDAPatternParser parser = new TDAPatternParser(errors, builder);
		TDAParsing canContinue = parser.tryParsing(line);
		assertNull(canContinue);
	}

	@Test
	public void trivialConstructorMatchRequiresCCB() {
		final Tokenizable line = line("(Nil {");
		context.checking(new Expectations() {{
			oneOf(errorsMock).message(line, "invalid pattern");
		}});
		TDAPatternParser parser = new TDAPatternParser(errors, builder);
		TDAParsing canContinue = parser.tryParsing(line);
		assertNull(canContinue);
	}

	@Test
	public void trivialConstructorMatchRequiresCRBAfterPattern() {
		final Tokenizable line = line("(Nil {}");
		context.checking(new Expectations() {{
			oneOf(builder).accept(with(CtorPatternMatcher.ctor("Nil")));
			oneOf(errorsMock).message(line, "invalid pattern");
		}});
		TDAPatternParser parser = new TDAPatternParser(errors, builder);
		TDAParsing canContinue = parser.tryParsing(line);
		assertNull(canContinue);
	}

	@Test
	public void trivialAConstructorCanHaveAParameter() {
		final Tokenizable line = line("(Cons {tail: Nil})");
		context.checking(new Expectations() {{
			oneOf(builder).accept(with(CtorPatternMatcher.ctor("Cons").field("tail", CtorPatternMatcher.ctor("Nil"))));
		}});
		TDAPatternParser parser = new TDAPatternParser(errors, builder);
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
		TDAPatternParser parser = new TDAPatternParser(errors, builder);
		TDAParsing canContinue = parser.tryParsing(line);
		assertNotNull(canContinue);
		assertNull(parser.tryParsing(line));
		assertFalse(line.hasMore());
	}

	@Test
	public void aTypeCanHaveParameters() {
		final Tokenizable line = line("(Cons[Number] nl)");
		context.checking(new Expectations() {{
			oneOf(builder).accept(with(TypedPatternMatcher.typed("Cons", "nl")));
		}});
		TDAPatternParser parser = new TDAPatternParser(errors, builder);
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
		TDAPatternParser parser = new TDAPatternParser(errors, builder);
		TDAParsing canContinue = parser.tryParsing(line);
		assertNull(canContinue);
	}

	@Test
	public void aTypeWithParametersMustBeInParens() {
		final Tokenizable line = line("Cons[Number]");
		context.checking(new Expectations() {{
			oneOf(builder).accept(with(CtorPatternMatcher.ctor("Cons")));
			oneOf(errorsMock).message(line, "invalid pattern");
		}});
		TDAPatternParser parser = new TDAPatternParser(errors, builder);
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
		TDAPatternParser parser = new TDAPatternParser(errors, builder);
		TDAParsing canContinue = parser.tryParsing(line);
		assertNull(canContinue);
	}

	@Test
	public void aTypeCanHaveMultipleParameters() {
		final Tokenizable line = line("(Map[Number,String] map)");
		context.checking(new Expectations() {{
			oneOf(builder).accept(with(TypedPatternMatcher.typed("Map", "map")));
		}});
		TDAPatternParser parser = new TDAPatternParser(errors, builder);
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
		TDAPatternParser parser = new TDAPatternParser(errors, builder);
		TDAParsing canContinue = parser.tryParsing(line);
		assertNull(canContinue);
	}

	@Test
	public void aTypeParameterCannotSuddenlyLaunchIntoSyntax() {
		final Tokenizable line = line("(Map[Number{}] map)");
		context.checking(new Expectations() {{
			oneOf(errorsMock).message(line, "invalid pattern");
		}});
		TDAPatternParser parser = new TDAPatternParser(errors, builder);
		TDAParsing canContinue = parser.tryParsing(line);
		assertNull(canContinue);
	}

	@Test
	public void aTypeParameterMustBeClosed() {
		final Tokenizable line = line("(Map[Number) map)");
		context.checking(new Expectations() {{
			oneOf(errorsMock).message(line, "invalid pattern");
		}});
		TDAPatternParser parser = new TDAPatternParser(errors, builder);
		TDAParsing canContinue = parser.tryParsing(line);
		assertNull(canContinue);
	}

	@Test
	public void aTypeParameterMustBeClosedBeforeVar() {
		final Tokenizable line = line("(Map[Number map)");
		context.checking(new Expectations() {{
			oneOf(errorsMock).message(line, "invalid pattern");
		}});
		TDAPatternParser parser = new TDAPatternParser(errors, builder);
		TDAParsing canContinue = parser.tryParsing(line);
		assertNull(canContinue);
	}

//	@Test
	public void howHardCanItBeToNestPatterns() {
		final Tokenizable line = line("(Cons { head: Nil })");
		context.checking(new Expectations() {{
			oneOf(builder).accept(with(TypedPatternMatcher.typed("Map", "map")));
		}});
		TDAPatternParser parser = new TDAPatternParser(errors, builder);
		TDAParsing canContinue = parser.tryParsing(line);
		assertNotNull(canContinue);
		assertNull(parser.tryParsing(line));
	}

	// TODO: don't forget nested patterns
	// Also special case of lists: []
	// Also special case of tuples: (a,b)
	
	public static Tokenizable line(String string) {
		return new Tokenizable(TDAStoryTests.line(string));
	}
}
