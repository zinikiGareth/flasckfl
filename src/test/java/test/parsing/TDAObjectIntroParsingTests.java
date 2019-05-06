package test.parsing;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.flasck.flas.commonBase.names.PackageName;
import org.flasck.flas.commonBase.names.SolidName;
import org.flasck.flas.compiler.ScopeReceiver;
import org.flasck.flas.errors.ErrorReporter;
import org.flasck.flas.parsedForm.FieldsDefn;
import org.flasck.flas.parser.IgnoreNestedParser;
import org.flasck.flas.parser.TDAIntroParser;
import org.flasck.flas.parser.TDAObjectElementsParser;
import org.flasck.flas.parser.TDAParsing;
import org.flasck.flas.parser.TopLevelDefnConsumer;
import org.flasck.flas.tokenizers.Tokenizable;
import org.jmock.Expectations;
import org.jmock.integration.junit4.JUnitRuleMockery;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

public class TDAObjectIntroParsingTests {
	@Rule public JUnitRuleMockery context = new JUnitRuleMockery();
	private ErrorReporter errors = context.mock(ErrorReporter.class);
	private TopLevelDefnConsumer builder = context.mock(TopLevelDefnConsumer.class);

	@Before
	public void setup() {
		context.checking(new Expectations() {{
			allowing(builder).scopeTo(with(any(ScopeReceiver.class)));
		}});
	}

	@Test
	public void theSimplestObjectCreatesAScopeEntryAndReturnsAFieldParser() {
		context.checking(new Expectations() {{
			allowing(builder).qualifyName("Store"); will(returnValue(new SolidName(null, "Store")));
			oneOf(builder).newObject(with(ObjectDefnMatcher.match("Store")));
		}});
		TDAIntroParser parser = new TDAIntroParser(errors, builder);
		TDAParsing nested = parser.tryParsing(TDABasicIntroParsingTests.line("object Store"));
		assertTrue(nested instanceof TDAObjectElementsParser);
	}

	/*
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
			allowing(builder).qualifyName("Cons"); will(returnValue(new SolidName(null, "Cons")));
			oneOf(builder).newObject(with(ObjectDefnMatcher.match("Cons").poly("A").locs(0,7)));
		}});
		TDAIntroParser parser = new TDAIntroParser(errors, builder);
		TDAParsing nested = parser.tryParsing(TDABasicIntroParsingTests.line("object Cons A"));
		assertTrue(nested instanceof TDAObjectFieldParser);
	}

	@Test
	public void polymorphicVarsMustBeValid() {
		final Tokenizable toks = TDABasicIntroParsingTests.line("object Cons xx");
		context.checking(new Expectations() {{
			oneOf(errors).message(toks, "invalid type argument");
		}});
		TDAIntroParser parser = new TDAIntroParser(errors, builder);
		TDAParsing nested = parser.tryParsing(toks);
		assertNull(nested);
	}

	@Test
	public void weCanTellAnEntityApartFromAObject() {
		context.checking(new Expectations() {{
			allowing(builder).qualifyName("Fred"); will(returnValue(new SolidName(new PackageName("test.names"), "Fred")));
			oneOf(builder).newObject(with(ObjectDefnMatcher.match("test.names.Fred").locs(0,7).as(FieldsDefn.FieldsType.ENTITY)));
		}});
		TDAIntroParser parser = new TDAIntroParser(errors, builder);
		TDAParsing nested = parser.tryParsing(TDABasicIntroParsingTests.line("entity Fred"));
		assertTrue(nested instanceof TDAObjectFieldParser);
	}

	@Test
	public void objectsInPackagesHaveQualifiedNames() {
		context.checking(new Expectations() {{
			allowing(builder).qualifyName("InPackage"); will(returnValue(new SolidName(new PackageName("test.names"), "InPackage")));
			oneOf(builder).newObject(with(ObjectDefnMatcher.match("test.names.InPackage")));
		}});
		TDAIntroParser parser = new TDAIntroParser(errors, builder);
		TDAParsing nested = parser.tryParsing(TDABasicIntroParsingTests.line("object InPackage"));
		assertTrue(nested instanceof TDAObjectFieldParser);
	}
	*/
}
