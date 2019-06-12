package test.parsing;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.flasck.flas.commonBase.names.PackageName;
import org.flasck.flas.commonBase.names.SolidName;
import org.flasck.flas.errors.ErrorReporter;
import org.flasck.flas.parsedForm.FunctionCaseDefn;
import org.flasck.flas.parser.IgnoreNestedParser;
import org.flasck.flas.parser.TDAIntroParser;
import org.flasck.flas.parser.TDAParsing;
import org.flasck.flas.parser.TopLevelDefnConsumer;
import org.flasck.flas.stories.TDAMultiParser;
import org.flasck.flas.tokenizers.Tokenizable;
import org.jmock.Expectations;
import org.jmock.integration.junit4.JUnitRuleMockery;
import org.junit.Rule;
import org.junit.Test;

public class TDAObjectIntroParsingTests {
	@Rule public JUnitRuleMockery context = new JUnitRuleMockery();
	private ErrorReporter errors = context.mock(ErrorReporter.class);
	private LocalErrorTracker tracker = new LocalErrorTracker(errors);
	private TopLevelDefnConsumer builder = context.mock(TopLevelDefnConsumer.class);

	@Test
	public void theSimplestObjectCreatesAScopeEntryAndReturnsAFieldParser() {
		context.checking(new Expectations() {{
			allowing(builder).qualifyName("Store"); will(returnValue(new SolidName(null, "Store")));
			oneOf(builder).newObject(with(ObjectDefnMatcher.match("Store")));
		}});
		TDAIntroParser parser = new TDAIntroParser(errors, builder);
		TDAParsing nested = parser.tryParsing(TDABasicIntroParsingTests.line("object Store"));
		assertTrue(nested instanceof TDAMultiParser);
	}

	@Test
	public void anObjectCanIncludeAFunction() {
		context.checking(new Expectations() {{
			allowing(builder).qualifyName("Store"); will(returnValue(new SolidName(null, "Store")));
			oneOf(builder).newObject(with(ObjectDefnMatcher.match("Store")));
			oneOf(builder).functionCase(with(any(FunctionCaseDefn.class)));
		}});
		TDAIntroParser parser = new TDAIntroParser(tracker, builder);
		TDAParsing nested = parser.tryParsing(TDABasicIntroParsingTests.line("object Store"));
		assertTrue(nested instanceof TDAMultiParser);
		nested.tryParsing(TDABasicIntroParsingTests.line("f = 42"));
		nested.scopeComplete(null);
	}

	@Test
	public void thereMustBeATypeName() {
		Tokenizable toks = TDABasicIntroParsingTests.line("object");
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
		Tokenizable toks = TDABasicIntroParsingTests.line("object fred");
		context.checking(new Expectations() {{
			oneOf(errors).message(toks, "invalid or missing type name");
		}});
		TDAIntroParser parser = new TDAIntroParser(errors, builder);
		TDAParsing nested = parser.tryParsing(toks);
		assertNotNull(nested);
		assertTrue(nested instanceof IgnoreNestedParser);
	}

	@Test
	public void aPolymorphicObjectDefinitionCreatesTheRightScopeEntryAndReturnsAFieldParser() {
		context.checking(new Expectations() {{
			allowing(builder).qualifyName("Store"); will(returnValue(new SolidName(null, "Store")));
			oneOf(builder).newObject(with(ObjectDefnMatcher.match("Store").poly("A").locs(0,7)));
		}});
		TDAIntroParser parser = new TDAIntroParser(errors, builder);
		TDAParsing nested = parser.tryParsing(TDABasicIntroParsingTests.line("object Store A"));
		assertTrue(nested instanceof TDAMultiParser);
	}

	@Test
	public void aPolymorphicObjectDefinitionCannotBeInBrackets() {
		final Tokenizable toks = TDABasicIntroParsingTests.line("object Store[A]");
		context.checking(new Expectations() {{
			oneOf(errors).message(with(TokenizableMatcher.match("[A]")), with("syntax error"));
		}});
		TDAIntroParser parser = new TDAIntroParser(errors, builder);
		TDAParsing nested = parser.tryParsing(toks);
		assertTrue(nested instanceof IgnoreNestedParser);
	}

	@Test
	public void polymorphicVarsMustBeValid() {
		final Tokenizable toks = TDABasicIntroParsingTests.line("object Store xx");
		context.checking(new Expectations() {{
			oneOf(errors).message(toks, "syntax error");
		}});
		TDAIntroParser parser = new TDAIntroParser(errors, builder);
		TDAParsing nested = parser.tryParsing(toks);
		assertTrue(nested instanceof IgnoreNestedParser);
	}

	@Test
	public void objectsInPackagesHaveQualifiedNames() {
		context.checking(new Expectations() {{
			allowing(builder).qualifyName("InPackage"); will(returnValue(new SolidName(new PackageName("test.names"), "InPackage")));
			oneOf(builder).newObject(with(ObjectDefnMatcher.match("test.names.InPackage")));
		}});
		TDAIntroParser parser = new TDAIntroParser(errors, builder);
		TDAParsing nested = parser.tryParsing(TDABasicIntroParsingTests.line("object InPackage"));
		assertTrue(nested instanceof TDAMultiParser);
	}
}
