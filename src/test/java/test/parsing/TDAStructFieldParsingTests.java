package test.parsing;

import static org.junit.Assert.assertNull;

import org.flasck.flas.errors.ErrorReporter;
import org.flasck.flas.parser.StructFieldConsumer;
import org.flasck.flas.parser.TDAParsing;
import org.flasck.flas.parser.TDAStructFieldParser;
import org.flasck.flas.tokenizers.Tokenizable;
import org.jmock.Expectations;
import org.jmock.integration.junit4.JUnitRuleMockery;
import org.junit.Rule;
import org.junit.Test;

public class TDAStructFieldParsingTests {
	@Rule public JUnitRuleMockery context = new JUnitRuleMockery();
	private ErrorReporter errors = context.mock(ErrorReporter.class);
	private StructFieldConsumer builder = context.mock(StructFieldConsumer.class);

	@Test
	public void aSimpleFieldDefinitionIsAdded() {
		context.checking(new Expectations() {{
			oneOf(builder).addField(with(StructFieldMatcher.match("A", "head")));
		}});
		TDAStructFieldParser parser = new TDAStructFieldParser(errors, builder);
		TDAParsing nested = parser.tryParsing(TDABasicIntroParsingTests.line("A head"));
		assertNull(nested);
	}

	@Test
	public void aDefinitionRemembersWhereItCameFrom() {
		context.checking(new Expectations() {{
			oneOf(builder).addField(with(StructFieldMatcher.match("String", "msg").locs(0, 7)));
		}});
		TDAStructFieldParser parser = new TDAStructFieldParser(errors, builder);
		TDAParsing nested = parser.tryParsing(TDABasicIntroParsingTests.line("String msg"));
		assertNull(nested);
	}

	@Test
	public void theFieldMustHaveAValidTypeName() {
		final Tokenizable toks = TDABasicIntroParsingTests.line("head");
		context.checking(new Expectations() {{
			oneOf(errors).message(toks, "field must have a valid type definition");
		}});
		TDAStructFieldParser parser = new TDAStructFieldParser(errors, builder);
		TDAParsing nested = parser.tryParsing(toks);
		assertNull(nested);
	}

	@Test
	public void theFieldMustHaveAValidVarName() {
		final Tokenizable toks = TDABasicIntroParsingTests.line("String A");
		context.checking(new Expectations() {{
			oneOf(errors).message(toks, "field must have a valid field name");
		}});
		TDAStructFieldParser parser = new TDAStructFieldParser(errors, builder);
		TDAParsing nested = parser.tryParsing(toks);
		assertNull(nested);
	}

	@Test
	public void theNameIDIsReserved() {
		final Tokenizable toks = TDABasicIntroParsingTests.line("String id");
		context.checking(new Expectations() {{
			oneOf(errors).message(toks, "'id' is a reserved field name");
		}});
		TDAStructFieldParser parser = new TDAStructFieldParser(errors, builder);
		TDAParsing nested = parser.tryParsing(toks);
		assertNull(nested);
	}

	@Test
	public void junkIsNotPermittedAtTheEndOfTheLine() {
		final Tokenizable toks = TDABasicIntroParsingTests.line("String msg for");
		context.checking(new Expectations() {{
			oneOf(errors).message(toks, "invalid tokens after expression");
		}});
		TDAStructFieldParser parser = new TDAStructFieldParser(errors, builder);
		TDAParsing nested = parser.tryParsing(toks);
		assertNull(nested);
	}
}
