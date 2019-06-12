package test.parsing;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.commonBase.names.SolidName;
import org.flasck.flas.errors.ErrorReporter;
import org.flasck.flas.parser.IgnoreNestedParser;
import org.flasck.flas.parser.NoNestingParser;
import org.flasck.flas.parser.TDAIntroParser;
import org.flasck.flas.parser.TDAParsing;
import org.flasck.flas.parser.TDAUnionFieldParser;
import org.flasck.flas.parser.TopLevelDefnConsumer;
import org.flasck.flas.parser.UnionFieldConsumer;
import org.flasck.flas.tokenizers.Tokenizable;
import org.jmock.Expectations;
import org.jmock.integration.junit4.JUnitRuleMockery;
import org.junit.Rule;
import org.junit.Test;

public class TDAUnionParsingTests {
	@Rule public JUnitRuleMockery context = new JUnitRuleMockery();
	private ErrorReporter errors = context.mock(ErrorReporter.class);
	private TopLevelDefnConsumer builder = context.mock(TopLevelDefnConsumer.class);

	@Test
	public void theUnionKeywordAppearsOnALineWithAType() {
		context.checking(new Expectations() {{
			allowing(builder).qualifyName("Foo"); will(returnValue(new SolidName(null, "Foo")));
			oneOf(builder).newUnion(with(UnionDefnMatcher.match("Foo")));
		}});
		TDAIntroParser parser = new TDAIntroParser(errors, builder);
		TDAParsing nested = parser.tryParsing(TDABasicIntroParsingTests.line("union Foo"));
		assertTrue(nested instanceof TDAUnionFieldParser);
	}

	@Test
	public void aUnionDefinitionMayBePolymorphic() {
		context.checking(new Expectations() {{
			allowing(builder).qualifyName("List"); will(returnValue(new SolidName(null, "List")));
			oneOf(builder).newUnion(with(UnionDefnMatcher.match("List").poly("A")));
		}});
		TDAIntroParser parser = new TDAIntroParser(errors, builder);
		TDAParsing nested = parser.tryParsing(TDABasicIntroParsingTests.line("union List A"));
		assertTrue(nested instanceof TDAUnionFieldParser);
	}

	@Test
	public void thereMustBeATypeName() {
		Tokenizable toks = TDABasicIntroParsingTests.line("union");
		context.checking(new Expectations() {{
			oneOf(errors).message(toks, "invalid or missing type name");
		}});
		TDAIntroParser parser = new TDAIntroParser(errors, builder);
		TDAParsing nested = parser.tryParsing(toks);
		assertNotNull(nested);
		assertTrue(nested instanceof IgnoreNestedParser);
	}

	@Test
	public void theTypeNameMustBeTheValidKind() {
		Tokenizable toks = TDABasicIntroParsingTests.line("union fred");
		context.checking(new Expectations() {{
			oneOf(errors).message(toks, "invalid or missing type name");
		}});
		TDAIntroParser parser = new TDAIntroParser(errors, builder);
		TDAParsing nested = parser.tryParsing(toks);
		assertNotNull(nested);
		assertTrue(nested instanceof IgnoreNestedParser);
	}

	@Test
	public void polymorphicVarsMustBeWhatTheyClaim() {
		final Tokenizable toks = TDABasicIntroParsingTests.line("union List a");
		context.checking(new Expectations() {{
			oneOf(errors).message(toks, "invalid type argument");
		}});
		TDAIntroParser parser = new TDAIntroParser(errors, builder);
		TDAParsing nested = parser.tryParsing(toks);
		assertTrue(nested instanceof IgnoreNestedParser);
	}

	@Test
	public void polymorphicVarsCannotBeRealTypes() {
		final Tokenizable toks = TDABasicIntroParsingTests.line("union List Croset");
		context.checking(new Expectations() {{
			oneOf(errors).message(toks, "invalid type argument");
		}});
		TDAIntroParser parser = new TDAIntroParser(errors, builder);
		TDAParsing nested = parser.tryParsing(toks);
		assertTrue(nested instanceof IgnoreNestedParser);
	}

	@Test
	public void unionFieldsCanBeSimpleTypes() {
		final UnionFieldConsumer consumer = context.mock(UnionFieldConsumer.class);
		context.checking(new Expectations() {{
			oneOf(consumer).addCase(with(TypeReferenceMatcher.type("Nil")));
		}});
		TDAUnionFieldParser parser = new TDAUnionFieldParser(errors, consumer);
		TDAParsing nested = parser.tryParsing(TDABasicIntroParsingTests.line("Nil"));
		assertTrue(nested instanceof NoNestingParser);
	}

	@Test
	public void unionFieldsCanHavePolyVars() {
		final UnionFieldConsumer consumer = context.mock(UnionFieldConsumer.class);
		context.checking(new Expectations() {{
			oneOf(consumer).addCase(with(TypeReferenceMatcher.type("Cons").poly("A")));
		}});
		TDAUnionFieldParser parser = new TDAUnionFieldParser(errors, consumer);
		TDAParsing nested = parser.tryParsing(TDABasicIntroParsingTests.line("Cons[A]"));
		assertTrue(nested instanceof NoNestingParser);
	}

	@Test
	public void unionFieldsCannotHaveFieldNames() {
		final UnionFieldConsumer consumer = context.mock(UnionFieldConsumer.class);
		final Tokenizable toks = TDABasicIntroParsingTests.line("Nil name");
		context.checking(new Expectations() {{
			oneOf(errors).message(toks, "tokens beyond end of line");
		}});
		TDAUnionFieldParser parser = new TDAUnionFieldParser(errors, consumer);
		TDAParsing nested = parser.tryParsing(toks);
		assertTrue(nested instanceof IgnoreNestedParser);
	}

	@Test
	public void unionFieldParserDoesNothingAtEnd() {
		TDAUnionFieldParser parser = new TDAUnionFieldParser(errors, null);
		parser.scopeComplete(new InputPosition("-", 10, 0, "hello"));
	}
}
