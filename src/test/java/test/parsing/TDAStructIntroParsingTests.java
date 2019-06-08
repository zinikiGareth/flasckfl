package test.parsing;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.flasck.flas.commonBase.names.PackageName;
import org.flasck.flas.commonBase.names.SolidName;
import org.flasck.flas.compiler.ScopeReceiver;
import org.flasck.flas.errors.ErrorReporter;
import org.flasck.flas.parsedForm.FieldsDefn;
import org.flasck.flas.parser.IgnoreNestedParser;
import org.flasck.flas.parser.TDAIntroParser;
import org.flasck.flas.parser.TDAParsing;
import org.flasck.flas.parser.TDAStructFieldParser;
import org.flasck.flas.parser.TopLevelDefnConsumer;
import org.flasck.flas.tokenizers.Tokenizable;
import org.jmock.Expectations;
import org.jmock.integration.junit4.JUnitRuleMockery;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

public class TDAStructIntroParsingTests {
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
	public void theSimplestStructCreatesAScopeEntryAndReturnsAFieldParser() {
		context.checking(new Expectations() {{
			allowing(builder).qualifyName("Nil"); will(returnValue(new SolidName(null, "Nil")));
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
		assertNotNull(nested);
		assertTrue(nested instanceof IgnoreNestedParser);
	}

	@Test
	public void theTypeNameMustBeTheValidKind() {
		Tokenizable toks = TDABasicIntroParsingTests.line("struct fred");
		context.checking(new Expectations() {{
			oneOf(errors).message(toks, "invalid or missing type name");
		}});
		TDAIntroParser parser = new TDAIntroParser(errors, builder);
		TDAParsing nested = parser.tryParsing(toks);
		assertNotNull(nested);
		assertTrue(nested instanceof IgnoreNestedParser);
	}

	@Test
	public void aPolymorphicStructDefinitionCreatesTheRightScopeEntryAndReturnsAFieldParser() {
		context.checking(new Expectations() {{
			allowing(builder).qualifyName("Cons"); will(returnValue(new SolidName(null, "Cons")));
			oneOf(builder).newStruct(with(StructDefnMatcher.match("Cons").poly("A").locs(0,7)));
		}});
		TDAIntroParser parser = new TDAIntroParser(errors, builder);
		TDAParsing nested = parser.tryParsing(TDABasicIntroParsingTests.line("struct Cons A"));
		assertTrue(nested instanceof TDAStructFieldParser);
	}

	@Test
	public void aPolymorphicStructDefinitionMayHaveMultipleVars() {
		context.checking(new Expectations() {{
			allowing(builder).qualifyName("Map"); will(returnValue(new SolidName(null, "Map")));
			oneOf(builder).newStruct(with(StructDefnMatcher.match("Map").poly("A").poly("B").locs(0,7)));
		}});
		TDAIntroParser parser = new TDAIntroParser(errors, builder);
		TDAParsing nested = parser.tryParsing(TDABasicIntroParsingTests.line("struct Map A B"));
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
		assertTrue(nested instanceof IgnoreNestedParser);
	}

	@Test
	public void weCanTellAnEntityApartFromAStruct() {
		context.checking(new Expectations() {{
			allowing(builder).qualifyName("Fred"); will(returnValue(new SolidName(new PackageName("test.names"), "Fred")));
			oneOf(builder).newStruct(with(StructDefnMatcher.match("test.names.Fred").locs(0,7).as(FieldsDefn.FieldsType.ENTITY)));
		}});
		TDAIntroParser parser = new TDAIntroParser(errors, builder);
		TDAParsing nested = parser.tryParsing(TDABasicIntroParsingTests.line("entity Fred"));
		assertTrue(nested instanceof TDAStructFieldParser);
	}

	@Test
	public void envelopesCanBeDefinedInTheSameWay() {
		context.checking(new Expectations() {{
			allowing(builder).qualifyName("Fred"); will(returnValue(new SolidName(new PackageName("test.names"), "Fred")));
			oneOf(builder).newStruct(with(StructDefnMatcher.match("test.names.Fred").locs(0,9).as(FieldsDefn.FieldsType.ENVELOPE)));
		}});
		TDAIntroParser parser = new TDAIntroParser(errors, builder);
		TDAParsing nested = parser.tryParsing(TDABasicIntroParsingTests.line("envelope Fred"));
		assertTrue(nested instanceof TDAStructFieldParser);
	}

	@Test
	public void wrapsAreVerySimilarToo() {
		context.checking(new Expectations() {{
			allowing(builder).qualifyName("Fred"); will(returnValue(new SolidName(new PackageName("test.names"), "Fred")));
			allowing(builder).qualifyName("InstanceOfFred"); will(returnValue(new SolidName(new PackageName("test.names"), "InstanceOfFred")));
			oneOf(builder).newStruct(with(StructDefnMatcher.match("test.names.Fred").locs(0,6).as(FieldsDefn.FieldsType.WRAPS)));
		}});
		TDAIntroParser parser = new TDAIntroParser(errors, builder);
		TDAParsing nested = parser.tryParsing(TDABasicIntroParsingTests.line("wraps Fred <- InstanceOfFred"));
		assertTrue(nested instanceof TDAStructFieldParser);
	}

	@Test
	public void structsInPackagesHaveQualifiedNames() {
		context.checking(new Expectations() {{
			allowing(builder).qualifyName("InPackage"); will(returnValue(new SolidName(new PackageName("test.names"), "InPackage")));
			oneOf(builder).newStruct(with(StructDefnMatcher.match("test.names.InPackage")));
		}});
		TDAIntroParser parser = new TDAIntroParser(errors, builder);
		TDAParsing nested = parser.tryParsing(TDABasicIntroParsingTests.line("struct InPackage"));
		assertTrue(nested instanceof TDAStructFieldParser);
	}
}
