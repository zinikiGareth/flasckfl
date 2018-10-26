package test.parsing;

import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.flasck.flas.errors.ErrorReporter;
import org.flasck.flas.parser.TDAIntroParser;
import org.flasck.flas.parser.TDAParsing;
import org.flasck.flas.parser.TDAStructFieldParser;
import org.flasck.flas.parser.TopLevelDefnConsumer;
import org.flasck.flas.tokenizers.Tokenizable;
import org.jmock.Expectations;
import org.jmock.integration.junit4.JUnitRuleMockery;
import org.junit.Rule;
import org.junit.Test;

public class TDAStructIntroParsingTests {
	@Rule public JUnitRuleMockery context = new JUnitRuleMockery();
	private ErrorReporter errors = context.mock(ErrorReporter.class);
	private TopLevelDefnConsumer builder = context.mock(TopLevelDefnConsumer.class);

	@Test
	public void theSimplestStructCreatesAScopeEntryAndReturnsAFieldParser() {
		context.checking(new Expectations() {{
			oneOf(builder).newStruct(with(StructDefnMatcher.match("Nil")));
		}});
		TDAIntroParser parser = new TDAIntroParser(errors, builder);
		TDAParsing nested = parser.tryParsing(TDABasicIntroParsingTests.line("struct Nil"));
		assertTrue(nested instanceof TDAStructFieldParser);
	}

	@Test
	public void thereMustBeATypeName() {
		Tokenizable toks = TDABasicIntroParsingTests.line("struct");
		context.checking(new Expectations() {{
			oneOf(errors).message(toks, "invalid or missing type name");
		}});
		TDAIntroParser parser = new TDAIntroParser(errors, builder);
		TDAParsing nested = parser.tryParsing(toks);
		assertNull(nested);
	}

	@Test
	public void theTypeNameMustBeTheValidKind() {
		Tokenizable toks = TDABasicIntroParsingTests.line("struct fred");
		context.checking(new Expectations() {{
			oneOf(errors).message(toks, "invalid or missing type name");
		}});
		TDAIntroParser parser = new TDAIntroParser(errors, builder);
		TDAParsing nested = parser.tryParsing(toks);
		assertNull(nested);
	}

	@Test
	public void aPolymorphicStructDefinitionCreatesTheRightScopeEntryAndReturnsAFieldParser() {
		context.checking(new Expectations() {{
			oneOf(builder).newStruct(with(StructDefnMatcher.match("Cons").poly("A")));
		}});
		TDAIntroParser parser = new TDAIntroParser(errors, builder);
		TDAParsing nested = parser.tryParsing(TDABasicIntroParsingTests.line("struct Cons A"));
		assertTrue(nested instanceof TDAStructFieldParser);
	}

	@Test
	public void polymorphicVarsMustBeValid() {
		final Tokenizable toks = TDABasicIntroParsingTests.line("struct Cons xx");
		context.checking(new Expectations() {{
			oneOf(errors).message(toks, "invalid type argument");
		}});
		TDAIntroParser parser = new TDAIntroParser(errors, builder);
		TDAParsing nested = parser.tryParsing(toks);
		assertNull(nested);
	}
}
