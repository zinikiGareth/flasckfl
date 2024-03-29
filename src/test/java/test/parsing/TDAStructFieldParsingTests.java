package test.parsing;

import static org.junit.Assert.assertTrue;

import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.errors.ErrorReporter;
import org.flasck.flas.errors.LocalErrorTracker;
import org.flasck.flas.grammar.tracking.LoggableToken;
import org.flasck.flas.parsedForm.FieldsDefn.FieldsType;
import org.flasck.flas.parser.IgnoreNestedParser;
import org.flasck.flas.parser.LocationTracker;
import org.flasck.flas.parser.NoNestingParser;
import org.flasck.flas.parser.StructFieldConsumer;
import org.flasck.flas.parser.TDAParsing;
import org.flasck.flas.parser.TDAStructFieldParser;
import org.flasck.flas.testsupport.TestSupport;
import org.flasck.flas.testsupport.matchers.ExprMatcher;
import org.flasck.flas.testsupport.matchers.StringLiteralMatcher;
import org.flasck.flas.testsupport.matchers.StructFieldMatcher;
import org.flasck.flas.tokenizers.Tokenizable;
import org.jmock.Expectations;
import org.jmock.integration.junit4.JUnitRuleMockery;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.zinutils.support.jmock.ReturnInvoker;

public class TDAStructFieldParsingTests {
	@Rule public JUnitRuleMockery context = new JUnitRuleMockery();
	private ErrorReporter errors = context.mock(ErrorReporter.class);
	private LocalErrorTracker tracker = new LocalErrorTracker(errors);
	private StructFieldConsumer builder = context.mock(StructFieldConsumer.class);
	private LocationTracker locTracker = null;

	@Before
	public void ignoreParserLogging() {
		context.checking(new Expectations() {{
			allowing(errors).logParsingToken(with(any(LoggableToken.class))); will(ReturnInvoker.arg(0));
			allowing(errors).logReduction(with(any(String.class)), with(any(InputPosition.class)), with(any(InputPosition.class)));
		}});
	}

	@Test
	public void aSimpleFieldDefinitionIsAdded() {
		context.checking(new Expectations() {{
			allowing(builder).holder(); will(returnValue(null));
			oneOf(builder).addField(with(StructFieldMatcher.match("A", "head")));
		}});
		TDAStructFieldParser parser = new TDAStructFieldParser(tracker, builder, FieldsType.STRUCT, true, locTracker);
		TDAParsing nested = parser.tryParsing(TestSupport.tokline("A head"));
		assertTrue(nested instanceof NoNestingParser);
	}

	@Test
	public void aDefinitionRemembersWhereItCameFrom() {
		context.checking(new Expectations() {{
			allowing(builder).holder(); will(returnValue(null));
			oneOf(builder).addField(with(StructFieldMatcher.match("String", "msg").locs(0, 7)));
		}});
		TDAStructFieldParser parser = new TDAStructFieldParser(tracker, builder, FieldsType.STRUCT, true, locTracker);
		TDAParsing nested = parser.tryParsing(TestSupport.tokline("String msg"));
		assertTrue(nested instanceof NoNestingParser);
	}

	@Test
	public void theFieldMustHaveAValidTypeName() {
		final Tokenizable toks = TestSupport.tokline("head");
		context.checking(new Expectations() {{
			oneOf(errors).message(toks, "field must have a valid type definition");
		}});
		TDAStructFieldParser parser = new TDAStructFieldParser(tracker, builder, FieldsType.STRUCT, true, locTracker);
		TDAParsing nested = parser.tryParsing(toks);
		assertTrue(nested instanceof IgnoreNestedParser);
	}

	@Test
	public void theFieldMustHaveAValidVarName() {
		final Tokenizable toks = TestSupport.tokline("String A");
		context.checking(new Expectations() {{
			oneOf(errors).message(toks, "field must have a valid field name");
		}});
		TDAStructFieldParser parser = new TDAStructFieldParser(tracker, builder, FieldsType.STRUCT, true, locTracker);
		TDAParsing nested = parser.tryParsing(toks);
		assertTrue(nested instanceof IgnoreNestedParser);
	}

	@Test
	public void theNameIDIsReserved() {
		final Tokenizable toks = TestSupport.tokline("String id");
		context.checking(new Expectations() {{
			oneOf(errors).message(toks, "'id' is a reserved field name");
		}});
		TDAStructFieldParser parser = new TDAStructFieldParser(tracker, builder, FieldsType.STRUCT, true, locTracker);
		TDAParsing nested = parser.tryParsing(toks);
		assertTrue(nested instanceof IgnoreNestedParser);
	}

	@Test
	public void junkIsNotPermittedAtTheEndOfTheLine() {
		final Tokenizable toks = TestSupport.tokline("String msg for");
		context.checking(new Expectations() {{
			oneOf(errors).message(toks, "expected <- or end of line");
		}});
		TDAStructFieldParser parser = new TDAStructFieldParser(tracker, builder, FieldsType.STRUCT, true, locTracker);
		TDAParsing nested = parser.tryParsing(toks);
		assertTrue(nested instanceof IgnoreNestedParser);
	}

	@Test
	public void aFieldMayHaveAnInitializer() {
		context.checking(new Expectations() {{
			allowing(errors).hasErrors(); will(returnValue(false));
			allowing(builder).holder(); will(returnValue(null));
			oneOf(builder).addField(with(StructFieldMatcher.match("String", "msg").assign(11, new StringLiteralMatcher("foo"))));
		}});
		TDAStructFieldParser parser = new TDAStructFieldParser(tracker, builder, FieldsType.STRUCT, true, locTracker);
		TDAParsing nested = parser.tryParsing(TestSupport.tokline("String msg <- 'foo'"));
		assertTrue(nested instanceof NoNestingParser);
	}

	@Test
	public void junkIsNotPermittedAfterAnAssignment() {
		final Tokenizable toks = TestSupport.tokline("String msg <- 13)");
		context.checking(new Expectations() {{
			oneOf(builder).holder(); will(returnValue(null));
			oneOf(builder).addField(with(StructFieldMatcher.match("String", "msg").assign(11, ExprMatcher.number(13))));
			oneOf(errors).message(with(toks), with("invalid tokens after expression"));
		}});
		TDAStructFieldParser parser = new TDAStructFieldParser(tracker, builder, FieldsType.STRUCT, true, locTracker);
		TDAParsing nested = parser.tryParsing(toks);
		assertTrue(nested instanceof IgnoreNestedParser);
	}

	@Test
	public void envelopeFieldsMayNotHaveInitializers() {
		final Tokenizable toks = TestSupport.tokline("String msg <- 13");
		context.checking(new Expectations() {{
			oneOf(errors).message(toks, "envelope fields may not have initializers");
		}});
		TDAStructFieldParser parser = new TDAStructFieldParser(tracker, builder, FieldsType.ENVELOPE, false, locTracker);
		TDAParsing nested = parser.tryParsing(toks);
		assertTrue(nested instanceof IgnoreNestedParser);
	}

	@Test
	public void wrapsFieldsMustHaveInitializers() {
		final Tokenizable toks = TestSupport.tokline("msg");
		context.checking(new Expectations() {{
			oneOf(errors).message(toks, "wraps fields must have initializers");
		}});
		TDAStructFieldParser parser = new TDAStructFieldParser(tracker, builder, FieldsType.WRAPS, false, locTracker);
		TDAParsing nested = parser.tryParsing(toks);
		assertTrue(nested instanceof IgnoreNestedParser);
	}
}
