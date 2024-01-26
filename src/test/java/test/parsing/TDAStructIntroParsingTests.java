package test.parsing;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.blocker.TDAParsingWithAction;
import org.flasck.flas.errors.ErrorReporter;
import org.flasck.flas.errors.LocalErrorTracker;
import org.flasck.flas.grammar.tracking.LoggableToken;
import org.flasck.flas.parsedForm.FieldsDefn;
import org.flasck.flas.parser.IgnoreNestedParser;
import org.flasck.flas.parser.PackageNamer;
import org.flasck.flas.parser.TDAIntroParser;
import org.flasck.flas.parser.TDAParsing;
import org.flasck.flas.parser.TDAStructFieldParser;
import org.flasck.flas.parser.TopLevelDefinitionConsumer;
import org.flasck.flas.parser.TopLevelNamer;
import org.flasck.flas.testsupport.TestSupport;
import org.flasck.flas.testsupport.matchers.StructDefnMatcher;
import org.flasck.flas.tokenizers.Tokenizable;
import org.jmock.Expectations;
import org.jmock.integration.junit4.JUnitRuleMockery;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.zinutils.support.jmock.ReturnInvoker;

public class TDAStructIntroParsingTests {
	@Rule public JUnitRuleMockery context = new JUnitRuleMockery();
	private ErrorReporter errors = context.mock(ErrorReporter.class);
	private LocalErrorTracker tracker = new LocalErrorTracker(errors);
	private TopLevelDefinitionConsumer builder = context.mock(TopLevelDefinitionConsumer.class);
	private TopLevelNamer namer = new PackageNamer("test.names");

	@Before
	public void ignoreParserLogging() {
		context.checking(new Expectations() {{
			allowing(errors).logParsingToken(with(any(LoggableToken.class))); will(ReturnInvoker.arg(0));
			allowing(errors).logReduction(with(any(String.class)), with(any(InputPosition.class)), with(any(InputPosition.class)));
		}});
	}

	@Test
	public void theSimplestStructCreatesAScopeEntryAndReturnsAFieldParser() {
		context.checking(new Expectations() {{
			oneOf(builder).newStruct(with(tracker), with(StructDefnMatcher.match("test.names.Nil")));
		}});
		TDAIntroParser parser = new TDAIntroParser(tracker, namer, builder);
		TDAParsing nested = parser.tryParsing(TestSupport.tokline("struct Nil"));
		assertTrue(nested instanceof TDAParsingWithAction);
		assertTrue(((TDAParsingWithAction)nested).parser instanceof TDAStructFieldParser);
	}

	@Test
	public void thereMustBeATypeName() {
		Tokenizable toks = TestSupport.tokline("struct");
		context.checking(new Expectations() {{
			oneOf(errors).message(toks, "invalid or missing type name");
		}});
		TDAIntroParser parser = new TDAIntroParser(tracker, namer, builder);
		TDAParsing nested = parser.tryParsing(toks);
		assertNotNull(nested);
		assertTrue(nested instanceof IgnoreNestedParser);
	}

	@Test
	public void theTypeNameMustBeTheValidKind() {
		Tokenizable toks = TestSupport.tokline("struct fred");
		context.checking(new Expectations() {{
			oneOf(errors).message(toks, "invalid or missing type name");
		}});
		TDAIntroParser parser = new TDAIntroParser(tracker, namer, builder);
		TDAParsing nested = parser.tryParsing(toks);
		assertNotNull(nested);
		assertTrue(nested instanceof IgnoreNestedParser);
	}

	@Test
	public void aPolymorphicStructDefinitionCreatesTheRightScopeEntryAndReturnsAFieldParser() {
		context.checking(new Expectations() {{
			oneOf(builder).newStruct(with(tracker), with(StructDefnMatcher.match("test.names.Cons").poly("A").locs(0,7)));
		}});
		TDAIntroParser parser = new TDAIntroParser(tracker, namer, builder);
		TDAParsing nested = parser.tryParsing(TestSupport.tokline("struct Cons A"));
		assertTrue(nested instanceof TDAParsingWithAction);
		assertTrue(((TDAParsingWithAction)nested).parser instanceof TDAStructFieldParser);
	}

	@Test
	public void aPolymorphicStructDefinitionMayHaveMultipleVars() {
		context.checking(new Expectations() {{
			oneOf(builder).newStruct(with(tracker), with(StructDefnMatcher.match("test.names.Map").poly("A").poly("B").locs(0,7)));
		}});
		TDAIntroParser parser = new TDAIntroParser(tracker, namer, builder);
		TDAParsing nested = parser.tryParsing(TestSupport.tokline("struct Map A B"));
		assertTrue(nested instanceof TDAParsingWithAction);
		assertTrue(((TDAParsingWithAction)nested).parser instanceof TDAStructFieldParser);
	}

	@Test
	public void polymorphicVarsMustBeValid() {
		final Tokenizable toks = TestSupport.tokline("struct Cons xx");
		context.checking(new Expectations() {{
			oneOf(errors).message(toks, "invalid type argument");
		}});
		TDAIntroParser parser = new TDAIntroParser(tracker, namer, builder);
		TDAParsing nested = parser.tryParsing(toks);
		assertTrue(nested instanceof IgnoreNestedParser);
	}

	@Test
	public void weCanTellAnEntityApartFromAStruct() {
		context.checking(new Expectations() {{
			oneOf(builder).newStruct(with(tracker), with(StructDefnMatcher.match("test.names.Fred").locs(0,7).as(FieldsDefn.FieldsType.ENTITY)));
		}});
		TDAIntroParser parser = new TDAIntroParser(tracker, namer, builder);
		TDAParsing nested = parser.tryParsing(TestSupport.tokline("entity Fred"));
		assertTrue(nested instanceof TDAParsingWithAction);
		assertTrue(((TDAParsingWithAction)nested).parser instanceof TDAStructFieldParser);
	}

	@Test
	public void structsInPackagesHaveQualifiedNames() {
		context.checking(new Expectations() {{
			oneOf(builder).newStruct(with(tracker), with(StructDefnMatcher.match("test.names.InPackage")));
		}});
		TDAIntroParser parser = new TDAIntroParser(tracker, namer, builder);
		TDAParsing nested = parser.tryParsing(TestSupport.tokline("struct InPackage"));
		assertTrue(nested instanceof TDAParsingWithAction);
		assertTrue(((TDAParsingWithAction)nested).parser instanceof TDAStructFieldParser);
	}
}
