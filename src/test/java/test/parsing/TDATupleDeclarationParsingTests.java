package test.parsing;

import static org.junit.Assert.assertNull;

import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.errors.ErrorReporter;
import org.flasck.flas.parser.TDATupleDeclarationParser;
import org.flasck.flas.parser.TDAParsing;
import org.flasck.flas.parser.TopLevelDefnConsumer;
import org.flasck.flas.tokenizers.Tokenizable;
import org.jmock.Expectations;
import org.jmock.integration.junit4.JUnitRuleMockery;
import org.junit.Rule;
import org.junit.Test;

import test.flas.stories.TDAStoryTests;

public class TDATupleDeclarationParsingTests {
	@Rule public JUnitRuleMockery context = new JUnitRuleMockery();
	private ErrorReporter errors = context.mock(ErrorReporter.class);
	private TopLevelDefnConsumer builder = context.mock(TopLevelDefnConsumer.class);

	@Test
	public void aBlankLineReturnsNothingAndDoesNothing() {
		context.checking(new Expectations() {{
		}});
		TDATupleDeclarationParser parser = new TDATupleDeclarationParser(errors, builder);
		TDAParsing nested = parser.tryParsing(line(""));
		assertNull(nested);
	}

	@Test
	public void aLineWithoutAnOpeningParenDoesNotMatch() {
		context.checking(new Expectations() {{
		}});
		TDATupleDeclarationParser parser = new TDATupleDeclarationParser(errors, builder);
		TDAParsing nested = parser.tryParsing(line("+x"));
		assertNull(nested);
	}

	@Test
	public void aLineWithJustAnOpeningParenIsASyntaxError() {
		final Tokenizable line = line("(");
		context.checking(new Expectations() {{
			oneOf(errors).message(line, "syntax error");
		}});
		TDATupleDeclarationParser parser = new TDATupleDeclarationParser(errors, builder);
		TDAParsing nested = parser.tryParsing(line);
		assertNull(nested);
	}

	@Test
	public void aLineWithJustAnOpeningParenAndAVarIsASyntaxError() {
		final Tokenizable line = line("(x");
		context.checking(new Expectations() {{
			oneOf(errors).message(line, "syntax error");
		}});
		TDATupleDeclarationParser parser = new TDATupleDeclarationParser(errors, builder);
		TDAParsing nested = parser.tryParsing(line);
		assertNull(nested);
	}

	@Test
	public void aLineWithAnInvalidPuncCharIsASyntaxError() {
		final Tokenizable line = line("(}");
		context.checking(new Expectations() {{
			oneOf(errors).message(line, "syntax error");
		}});
		TDATupleDeclarationParser parser = new TDATupleDeclarationParser(errors, builder);
		TDAParsing nested = parser.tryParsing(line);
		assertNull(nested);
	}

	@Test
	public void aLineWithJustAnOpeningAndClosingParenIsAMissingVarsError() {
		final Tokenizable line = line("()");
		context.checking(new Expectations() {{
			oneOf(errors).message(line, "missing var in tuple declaration");
		}});
		TDATupleDeclarationParser parser = new TDATupleDeclarationParser(errors, builder);
		TDAParsing nested = parser.tryParsing(line);
		assertNull(nested);
	}

	@Test
	public void aLineWithJustOneVarIsANotATupleError() {
		final Tokenizable line = line("(x)");
		context.checking(new Expectations() {{
			oneOf(errors).message(line, "insufficient vars to make tuple declaration");
		}});
		TDATupleDeclarationParser parser = new TDATupleDeclarationParser(errors, builder);
		TDAParsing nested = parser.tryParsing(line);
		assertNull(nested);
	}

	public static Tokenizable line(String string) {
		return new Tokenizable(TDAStoryTests.line(string));
	}

}
