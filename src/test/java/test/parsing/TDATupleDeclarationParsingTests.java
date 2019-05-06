package test.parsing;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.util.List;

import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.commonBase.Expr;
import org.flasck.flas.commonBase.names.FunctionName;
import org.flasck.flas.compiler.ScopeReceiver;
import org.flasck.flas.errors.ErrorReporter;
import org.flasck.flas.parser.ParsedLineConsumer;
import org.flasck.flas.parser.TDAParsing;
import org.flasck.flas.parser.TDATupleDeclarationParser;
import org.flasck.flas.tokenizers.Tokenizable;
import org.jmock.Expectations;
import org.jmock.integration.junit4.JUnitRuleMockery;
import org.junit.Rule;
import org.junit.Test;

import test.flas.stories.TDAStoryTests;

public class TDATupleDeclarationParsingTests {
	@Rule public JUnitRuleMockery context = new JUnitRuleMockery();
	private ErrorReporter errors = context.mock(ErrorReporter.class);
	private ParsedLineConsumer builder = context.mock(ParsedLineConsumer.class);

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

	@Test
	public void aLineCannotHaveACommaWithoutAVar() {
		final Tokenizable line = line("(,");
		context.checking(new Expectations() {{
			oneOf(errors).message(line, "syntax error");
		}});
		TDATupleDeclarationParser parser = new TDATupleDeclarationParser(errors, builder);
		TDAParsing nested = parser.tryParsing(line);
		assertNull(nested);
	}

	@Test
	public void variablesMustBeSeparatedByCommas() {
		final Tokenizable line = line("(x:y) = ");
		context.checking(new Expectations() {{
			oneOf(errors).message(line, "syntax error");
		}});
		TDATupleDeclarationParser parser = new TDATupleDeclarationParser(errors, builder);
		TDAParsing nested = parser.tryParsing(line);
		assertNull(nested);
	}

	@Test
	public void variablesCannotBeLiterals() {
		final Tokenizable line = line("(2,y) = ");
		context.checking(new Expectations() {{
			oneOf(errors).message(line, "syntax error");
		}});
		TDATupleDeclarationParser parser = new TDATupleDeclarationParser(errors, builder);
		TDAParsing nested = parser.tryParsing(line);
		assertNull(nested);
	}

	@Test
	public void aLineCannotEndInAComma() {
		final Tokenizable line = line("(x,");
		context.checking(new Expectations() {{
			oneOf(errors).message(line, "syntax error");
		}});
		TDATupleDeclarationParser parser = new TDATupleDeclarationParser(errors, builder);
		TDAParsing nested = parser.tryParsing(line);
		assertNull(nested);
	}

	@Test
	public void aLineMustHaveAVarAfterAComma() {
		final Tokenizable line = line("(x,)");
		context.checking(new Expectations() {{
			oneOf(errors).message(line, "syntax error");
		}});
		TDATupleDeclarationParser parser = new TDATupleDeclarationParser(errors, builder);
		TDAParsing nested = parser.tryParsing(line);
		assertNull(nested);
	}

	@Test
	public void aLineMustHaveAnEqualsSignAfterTheVars() {
		final Tokenizable line = line("(x,y)");
		context.checking(new Expectations() {{
			oneOf(errors).message(line, "syntax error");
		}});
		TDATupleDeclarationParser parser = new TDATupleDeclarationParser(errors, builder);
		TDAParsing nested = parser.tryParsing(line);
		assertNull(nested);
	}

	@Test
	public void aLineMustHaveAnExactEqualsSignAfterTheVars() {
		final Tokenizable line = line("(x,y) ++ ");
		context.checking(new Expectations() {{
			oneOf(errors).message(line, "syntax error");
		}});
		TDATupleDeclarationParser parser = new TDATupleDeclarationParser(errors, builder);
		TDAParsing nested = parser.tryParsing(line);
		assertNull(nested);
	}

	@Test
	public void aLineMustHaveAnExpressionAfterTheVars() {
		final Tokenizable line = line("(x,y) = ");
		context.checking(new Expectations() {{
			oneOf(errors).message(line, "tuple assignment requires expression");
		}});
		TDATupleDeclarationParser parser = new TDATupleDeclarationParser(errors, builder);
		TDAParsing nested = parser.tryParsing(line);
		assertNull(nested);
	}

	@SuppressWarnings("unchecked")
	@Test
	public void aLineCanExist() {
		final Tokenizable line = line("(x,y) = f 10");
		final FunctionName fnName = null;
		context.checking(new Expectations() {{
			oneOf(builder).functionName(with(any(InputPosition.class)), with("_tuple_x")); will(returnValue(fnName));
			oneOf(builder).tupleDefn(with(any(List.class)), with(fnName), with(any(Expr.class)));
			allowing(builder).scopeTo(with(any(ScopeReceiver.class)));
		}});
		TDATupleDeclarationParser parser = new TDATupleDeclarationParser(errors, builder);
		TDAParsing nested = parser.tryParsing(line);
		assertNotNull(nested);
	}

	public static Tokenizable line(String string) {
		return new Tokenizable(TDAStoryTests.line(string));
	}
}
