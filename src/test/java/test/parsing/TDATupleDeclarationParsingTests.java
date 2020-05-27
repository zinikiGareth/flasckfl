package test.parsing;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.util.List;

import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.commonBase.Expr;
import org.flasck.flas.commonBase.names.FunctionName;
import org.flasck.flas.errors.ErrorReporter;
import org.flasck.flas.parser.TopLevelDefinitionConsumer;
import org.flasck.flas.parser.FunctionNameProvider;
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
	private LocalErrorTracker tracker = new LocalErrorTracker(errors);
	private FunctionNameProvider functionNamer = context.mock(FunctionNameProvider.class);
	private TopLevelDefinitionConsumer builder = context.mock(TopLevelDefinitionConsumer.class);

	@Test
	public void aBlankLineReturnsNothingAndDoesNothing() {
		context.checking(new Expectations() {{
		}});
		TDATupleDeclarationParser parser = new TDATupleDeclarationParser(tracker, functionNamer, builder, null);
		TDAParsing nested = parser.tryParsing(line(""));
		assertNull(nested);
	}

	@Test
	public void aLineWithoutAnOpeningParenDoesNotMatch() {
		context.checking(new Expectations() {{
		}});
		TDATupleDeclarationParser parser = new TDATupleDeclarationParser(tracker, functionNamer, builder, null);
		TDAParsing nested = parser.tryParsing(line("+x"));
		assertNull(nested);
	}

	@Test
	public void aLineWithJustAnOpeningParenIsASyntaxError() {
		final Tokenizable line = line("(");
		context.checking(new Expectations() {{
			oneOf(errors).message(line, "syntax error");
		}});
		TDATupleDeclarationParser parser = new TDATupleDeclarationParser(tracker, functionNamer, builder, null);
		TDAParsing nested = parser.tryParsing(line);
		assertNull(nested);
	}

	@Test
	public void aLineWithJustAnOpeningParenAndAVarIsASyntaxError() {
		final Tokenizable line = line("(x");
		context.checking(new Expectations() {{
			oneOf(errors).message(line, "syntax error");
		}});
		TDATupleDeclarationParser parser = new TDATupleDeclarationParser(tracker, functionNamer, builder, null);
		TDAParsing nested = parser.tryParsing(line);
		assertNull(nested);
	}

	@Test
	public void aLineWithAnInvalidPuncCharIsASyntaxError() {
		final Tokenizable line = line("(}");
		context.checking(new Expectations() {{
			oneOf(errors).message(line, "syntax error");
		}});
		TDATupleDeclarationParser parser = new TDATupleDeclarationParser(tracker, functionNamer, builder, null);
		TDAParsing nested = parser.tryParsing(line);
		assertNull(nested);
	}

	@Test
	public void aLineWithJustAnOpeningAndClosingParenIsAMissingVarsError() {
		final Tokenizable line = line("()");
		context.checking(new Expectations() {{
			oneOf(errors).message(line, "missing var in tuple declaration");
		}});
		TDATupleDeclarationParser parser = new TDATupleDeclarationParser(tracker, functionNamer, builder, null);
		TDAParsing nested = parser.tryParsing(line);
		assertNull(nested);
	}

	@Test
	public void aLineWithJustOneVarIsANotATupleError() {
		final Tokenizable line = line("(x)");
		context.checking(new Expectations() {{
			oneOf(errors).message(line, "insufficient vars to make tuple declaration");
		}});
		TDATupleDeclarationParser parser = new TDATupleDeclarationParser(tracker, functionNamer, builder, null);
		TDAParsing nested = parser.tryParsing(line);
		assertNull(nested);
	}

	@Test
	public void aLineCannotHaveACommaWithoutAVar() {
		final Tokenizable line = line("(,");
		context.checking(new Expectations() {{
			oneOf(errors).message(line, "syntax error");
		}});
		TDATupleDeclarationParser parser = new TDATupleDeclarationParser(tracker, functionNamer, builder, null);
		TDAParsing nested = parser.tryParsing(line);
		assertNull(nested);
	}

	@Test
	public void variablesMustBeSeparatedByCommas() {
		final Tokenizable line = line("(x:y) = ");
		context.checking(new Expectations() {{
			oneOf(errors).message(line, "syntax error");
		}});
		TDATupleDeclarationParser parser = new TDATupleDeclarationParser(tracker, functionNamer, builder, null);
		TDAParsing nested = parser.tryParsing(line);
		assertNull(nested);
	}

	@Test
	public void variablesCannotBeLiterals() {
		final Tokenizable line = line("(2,y) = ");
		context.checking(new Expectations() {{
			oneOf(errors).message(line, "syntax error");
		}});
		TDATupleDeclarationParser parser = new TDATupleDeclarationParser(tracker, functionNamer, builder, null);
		TDAParsing nested = parser.tryParsing(line);
		assertNull(nested);
	}

	@Test
	public void aLineCannotEndInAComma() {
		final Tokenizable line = line("(x,");
		context.checking(new Expectations() {{
			oneOf(errors).message(line, "syntax error");
		}});
		TDATupleDeclarationParser parser = new TDATupleDeclarationParser(tracker, functionNamer, builder, null);
		TDAParsing nested = parser.tryParsing(line);
		assertNull(nested);
	}

	@Test
	public void aLineMustHaveAVarAfterAComma() {
		final Tokenizable line = line("(x,)");
		context.checking(new Expectations() {{
			oneOf(errors).message(line, "syntax error");
		}});
		TDATupleDeclarationParser parser = new TDATupleDeclarationParser(tracker, functionNamer, builder, null);
		TDAParsing nested = parser.tryParsing(line);
		assertNull(nested);
	}

	@Test
	public void aLineMustHaveAnEqualsSignAfterTheVars() {
		final Tokenizable line = line("(x,y)");
		context.checking(new Expectations() {{
			oneOf(errors).message(line, "syntax error");
		}});
		TDATupleDeclarationParser parser = new TDATupleDeclarationParser(tracker, functionNamer, builder, null);
		TDAParsing nested = parser.tryParsing(line);
		assertNull(nested);
	}

	@Test
	public void aLineMustHaveAnExactEqualsSignAfterTheVars() {
		final Tokenizable line = line("(x,y) ++ ");
		context.checking(new Expectations() {{
			oneOf(errors).message(line, "syntax error");
		}});
		TDATupleDeclarationParser parser = new TDATupleDeclarationParser(tracker, functionNamer, builder, null);
		TDAParsing nested = parser.tryParsing(line);
		assertNull(nested);
	}

	@Test
	public void aLineMustHaveAnExpressionAfterTheVars() {
		final Tokenizable line = line("(x,y) = ");
		context.checking(new Expectations() {{
			oneOf(errors).message(line, "tuple assignment requires expression");
		}});
		TDATupleDeclarationParser parser = new TDATupleDeclarationParser(tracker, functionNamer, builder, null);
		TDAParsing nested = parser.tryParsing(line);
		assertNull(nested);
	}

	@SuppressWarnings("unchecked")
	@Test
	public void aLineCanExist() {
		final Tokenizable line = line("(x,y) = f 10");
		final FunctionName tnName = FunctionName.function(line.realinfo(), null, "_tuple_x");
		final FunctionName fnName = FunctionName.function(line.realinfo(), null, "x");
		context.checking(new Expectations() {{
			oneOf(functionNamer).functionName(with(any(InputPosition.class)), with("_tuple_x")); will(returnValue(tnName));
			oneOf(functionNamer).functionName(with(any(InputPosition.class)), with("x")); will(returnValue(fnName));
			oneOf(builder).tupleDefn(with(tracker), with(any(List.class)), with(tnName), with(fnName), with(any(Expr.class)));
		}});
		TDATupleDeclarationParser parser = new TDATupleDeclarationParser(tracker, functionNamer, builder, null);
		TDAParsing nested = parser.tryParsing(line);
		assertNotNull(nested);
	}

	public static Tokenizable line(String string) {
		return new Tokenizable(TDAStoryTests.line(string));
	}
}
