package test.parsing;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.commonBase.Locatable;
import org.flasck.flas.errors.ErrorReporter;
import org.flasck.flas.grammar.tracking.LoggableToken;
import org.flasck.flas.parser.IgnoreNestedParser;
import org.flasck.flas.parser.NoNestingParser;
import org.flasck.flas.parser.PackageNamer;
import org.flasck.flas.parser.TDAIntroParser;
import org.flasck.flas.parser.TDAParsing;
import org.flasck.flas.parser.TDAUnionFieldParser;
import org.flasck.flas.parser.TopLevelDefinitionConsumer;
import org.flasck.flas.parser.TopLevelNamer;
import org.flasck.flas.parser.UnionFieldConsumer;
import org.flasck.flas.testsupport.TestSupport;
import org.flasck.flas.testsupport.matchers.TypeReferenceMatcher;
import org.flasck.flas.testsupport.matchers.UnionDefnMatcher;
import org.flasck.flas.tokenizers.Tokenizable;
import org.jmock.Expectations;
import org.jmock.integration.junit4.JUnitRuleMockery;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.zinutils.support.jmock.ReturnInvoker;

public class TDAUnionParsingTests {
	@Rule public JUnitRuleMockery context = new JUnitRuleMockery();
	private ErrorReporter errors = context.mock(ErrorReporter.class);
	private TopLevelDefinitionConsumer builder = context.mock(TopLevelDefinitionConsumer.class);
	private TopLevelNamer namer = new PackageNamer("test.pkg");
	private InputPosition pos = new InputPosition("-", 1, 0, null, "hello");

	@Before
	public void ignoreParserLogging() {
		context.checking(new Expectations() {{
			allowing(errors).logParsingToken(with(any(LoggableToken.class))); will(ReturnInvoker.arg(0));
			allowing(errors).logReduction(with(any(String.class)), with(any(InputPosition.class)), with(any(InputPosition.class)));
			allowing(errors).logReduction(with(any(String.class)), with(any(Locatable.class)), with(any(Locatable.class)));
		}});
	}

	@Test
	public void theUnionKeywordAppearsOnALineWithAType() {
		context.checking(new Expectations() {{
			oneOf(builder).newUnion(with(errors), with(UnionDefnMatcher.match("test.pkg.Foo")));
		}});
		TDAIntroParser parser = new TDAIntroParser(errors, namer, builder);
		TDAParsing nested = parser.tryParsing(TestSupport.tokline("union Foo"));
		assertTrue(nested instanceof TDAUnionFieldParser);
	}

	@Test
	public void aUnionDefinitionMayBePolymorphic() {
		context.checking(new Expectations() {{
			oneOf(builder).newUnion(with(errors), with(UnionDefnMatcher.match("test.pkg.List").poly("A")));
		}});
		TDAIntroParser parser = new TDAIntroParser(errors, namer, builder);
		TDAParsing nested = parser.tryParsing(TestSupport.tokline("union List A"));
		assertTrue(nested instanceof TDAUnionFieldParser);
	}

	@Test
	public void thereMustBeATypeName() {
		Tokenizable toks = TestSupport.tokline("union");
		context.checking(new Expectations() {{
			oneOf(errors).message(toks, "invalid or missing type name");
		}});
		TDAIntroParser parser = new TDAIntroParser(errors, namer, builder);
		TDAParsing nested = parser.tryParsing(toks);
		assertNotNull(nested);
		assertTrue(nested instanceof IgnoreNestedParser);
	}

	@Test
	public void theTypeNameMustBeTheValidKind() {
		Tokenizable toks = TestSupport.tokline("union fred");
		context.checking(new Expectations() {{
			oneOf(errors).message(toks, "invalid or missing type name");
		}});
		TDAIntroParser parser = new TDAIntroParser(errors, namer, builder);
		TDAParsing nested = parser.tryParsing(toks);
		assertNotNull(nested);
		assertTrue(nested instanceof IgnoreNestedParser);
	}

	@Test
	public void polymorphicVarsMustBeWhatTheyClaim() {
		final Tokenizable toks = TestSupport.tokline("union List a");
		context.checking(new Expectations() {{
			oneOf(errors).message(toks, "invalid type argument");
		}});
		TDAIntroParser parser = new TDAIntroParser(errors, namer, builder);
		TDAParsing nested = parser.tryParsing(toks);
		assertTrue(nested instanceof IgnoreNestedParser);
	}

	@Test
	public void polymorphicVarsCannotBeRealTypes() {
		final Tokenizable toks = TestSupport.tokline("union List Croset");
		context.checking(new Expectations() {{
			oneOf(errors).message(toks, "invalid type argument");
		}});
		TDAIntroParser parser = new TDAIntroParser(errors, namer, builder);
		TDAParsing nested = parser.tryParsing(toks);
		assertTrue(nested instanceof IgnoreNestedParser);
	}

	@Test
	public void unionFieldsCanBeSimpleTypes() {
		final UnionFieldConsumer consumer = context.mock(UnionFieldConsumer.class);
		context.checking(new Expectations() {{
			oneOf(consumer).addCase(with(TypeReferenceMatcher.type("Nil")));
		}});
		TDAUnionFieldParser parser = new TDAUnionFieldParser(errors, pos, consumer);
		TDAParsing nested = parser.tryParsing(TestSupport.tokline("Nil"));
		assertTrue(nested instanceof NoNestingParser);
	}

	@Test
	public void unionFieldsCanHavePolyVars() {
		final UnionFieldConsumer consumer = context.mock(UnionFieldConsumer.class);
		context.checking(new Expectations() {{
			oneOf(consumer).addCase(with(TypeReferenceMatcher.type("Cons").poly("A")));
		}});
		TDAUnionFieldParser parser = new TDAUnionFieldParser(errors, pos, consumer);
		TDAParsing nested = parser.tryParsing(TestSupport.tokline("Cons[A]"));
		assertTrue(nested instanceof NoNestingParser);
	}

	@Test
	public void unionFieldsCannotHaveFieldNames() {
		final UnionFieldConsumer consumer = context.mock(UnionFieldConsumer.class);
		final Tokenizable toks = TestSupport.tokline("Nil name");
		context.checking(new Expectations() {{
			oneOf(errors).message(toks, "tokens beyond end of line");
		}});
		TDAUnionFieldParser parser = new TDAUnionFieldParser(errors, pos, consumer);
		TDAParsing nested = parser.tryParsing(toks);
		assertTrue(nested instanceof IgnoreNestedParser);
	}

	@Test
	public void unionFieldParserDoesNothingAtEnd() {
		TDAUnionFieldParser parser = new TDAUnionFieldParser(errors, pos, null);
		parser.scopeComplete(new InputPosition("-", 10, 0, null, "hello"));
	}
}
