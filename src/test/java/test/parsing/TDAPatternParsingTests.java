package test.parsing;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.util.function.Consumer;

import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.commonBase.Pattern;
import org.flasck.flas.errors.ErrorReporter;
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
	private ErrorReporter errors = context.mock(ErrorReporter.class);
	@SuppressWarnings("unchecked")
	private Consumer<Pattern> builder = context.mock(Consumer.class);
	private InputPosition pos = new InputPosition("-", 1, 0, "hello");

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
			oneOf(errors).message(line, "invalid pattern");
		}});
		TDAPatternParser parser = new TDAPatternParser(errors, builder);
		TDAParsing canContinue = parser.tryParsing(line);
		assertNull(canContinue);
	}

	@Test
	public void openAndCloseIsASyntaxError() {
		final Tokenizable line = line("()");
		context.checking(new Expectations() {{
			oneOf(errors).message(line, "invalid pattern");
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
			oneOf(errors).message(line, "invalid pattern");
		}});
		TDAPatternParser parser = new TDAPatternParser(errors, builder);
		TDAParsing canContinue = parser.tryParsing(line);
		assertNull(canContinue);
	}

	@Test
	public void aConstructorByItselfIsJustAPatternWithNoArgs() {
		final Tokenizable line = line("Nil");
		context.checking(new Expectations() {{
			oneOf(builder).accept(with(CtorPatternMatcher.ctor("Nil")));
		}});
		TDAPatternParser parser = new TDAPatternParser(errors, builder);
		TDAParsing canContinue = parser.tryParsing(line);
		assertNotNull(canContinue);
		assertNull(parser.tryParsing(line));
	}

	// TODO: don't forget nested patterns
	public static Tokenizable line(String string) {
		return new Tokenizable(TDAStoryTests.line(string));
	}
}
