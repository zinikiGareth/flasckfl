package test.parsing;

import static org.junit.Assert.assertTrue;

import org.flasck.flas.commonBase.names.SolidName;
import org.flasck.flas.errors.ErrorReporter;
import org.flasck.flas.parsedForm.StateDefinition;
import org.flasck.flas.parsedForm.Template;
import org.flasck.flas.parser.FunctionScopeUnitConsumer;
import org.flasck.flas.parser.IgnoreNestedParser;
import org.flasck.flas.parser.ObjectElementsConsumer;
import org.flasck.flas.parser.TDAMethodMessageParser;
import org.flasck.flas.parser.TDAObjectElementsParser;
import org.flasck.flas.parser.TDAParsing;
import org.flasck.flas.parser.TDAStructFieldParser;
import org.flasck.flas.stories.TDAMultiParser;
import org.flasck.flas.tokenizers.Tokenizable;
import org.jmock.Expectations;
import org.jmock.integration.junit4.JUnitRuleMockery;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

public class TDAObjectElementParsingTests {
	@Rule public JUnitRuleMockery context = new JUnitRuleMockery();
	private ErrorReporter errorsMock = context.mock(ErrorReporter.class);
	private ObjectElementsConsumer builder = context.mock(ObjectElementsConsumer.class);
	private FunctionScopeUnitConsumer topLevel = context.mock(FunctionScopeUnitConsumer.class);
	
	@Before
	public void setup() {
		context.checking(new Expectations() {{
			allowing(builder).name(); will(returnValue(new SolidName(null, "MyObject")));
		}});
	}
	@Test
	public void junkIsNotAKeyword() {
		context.checking(new Expectations() {{
			oneOf(errorsMock).message(with(any(Tokenizable.class)), with("'junk' is not a valid object keyword"));
		}});
		TDAObjectElementsParser parser = new TDAObjectElementsParser(errorsMock, builder, topLevel);
		TDAParsing nested = parser.tryParsing(TDABasicIntroParsingTests.line("junk"));
		assertTrue(nested instanceof IgnoreNestedParser);
	}

	@Test
	public void objectsCanHaveAStateParser() {
		context.checking(new Expectations() {{
			oneOf(builder).defineState(with(any(StateDefinition.class)));
		}});
		TDAObjectElementsParser parser = new TDAObjectElementsParser(errorsMock, builder, topLevel);
		TDAParsing nested = parser.tryParsing(TDABasicIntroParsingTests.line("state"));
		assertTrue(nested instanceof TDAStructFieldParser);
	}
	
	@Test
	public void objectStateCannotHaveANameOrAnything() {
		context.checking(new Expectations() {{
			oneOf(errorsMock).message(with(any(Tokenizable.class)), with("extra characters at end of line"));
		}});
		TDAObjectElementsParser parser = new TDAObjectElementsParser(errorsMock, builder, topLevel);
		TDAParsing nested = parser.tryParsing(TDABasicIntroParsingTests.line("state Fred"));
		assertTrue(nested instanceof IgnoreNestedParser);
	}
	
	@Test
	public void objectsCanHaveAConstructor() {
		context.checking(new Expectations() {{
			allowing(errorsMock).hasErrors(); will(returnValue(false));
			oneOf(builder).addConstructor(with(ObjectCtorMatcher.called("simple")));
		}});
		TDAObjectElementsParser parser = new TDAObjectElementsParser(errorsMock, builder, topLevel);
		TDAParsing nested = parser.tryParsing(TDABasicIntroParsingTests.line("ctor simple"));
		assertTrue(nested instanceof TDAMethodMessageParser);
	}
	
	@Test
	public void objectsCanHaveAConstructorWithAnArgument() {
		context.checking(new Expectations() {{
			allowing(errorsMock).hasErrors(); will(returnValue(false));
			oneOf(builder).addConstructor(with(ObjectCtorMatcher.called("args").arg(PatternMatcher.var("x"))));
		}});
		TDAObjectElementsParser parser = new TDAObjectElementsParser(errorsMock, builder, topLevel);
		TDAParsing nested = parser.tryParsing(TDABasicIntroParsingTests.line("ctor args x"));
		assertTrue(nested instanceof TDAMethodMessageParser);
	}
	
	@Test
	public void objectsCanHaveAccessorMethods() { // Correct me if I'm wrong, but these are really functions, because they don't do state updates
		context.checking(new Expectations() {{
			allowing(errorsMock).hasErrors(); will(returnValue(false));
			oneOf(builder).addAccessor(with(ObjectAccessorMatcher.of(FunctionCaseMatcher.called(null, "myname"))));
		}});
		TDAObjectElementsParser parser = new TDAObjectElementsParser(errorsMock, builder, topLevel);
		TDAParsing nested = parser.tryParsing(TDABasicIntroParsingTests.line("acor myname = 42"));
		assertTrue(nested instanceof TDAMultiParser);
	}

	@Test
	public void objectsCanHaveAccessorMethodsWithFunctionArguments() {
		context.checking(new Expectations() {{
			allowing(errorsMock).hasErrors(); will(returnValue(false));
			oneOf(builder).addAccessor(with(ObjectAccessorMatcher.of(FunctionCaseMatcher.called(null, "myname"))));
		}});
		TDAObjectElementsParser parser = new TDAObjectElementsParser(errorsMock, builder, topLevel);
		TDAParsing nested = parser.tryParsing(TDABasicIntroParsingTests.line("acor myname x (Number y) = x + y"));
		assertTrue(nested instanceof TDAMultiParser);
	}

	@Test
	public void objectsCanHaveUpdateMethods() {
		context.checking(new Expectations() {{
			allowing(errorsMock).hasErrors(); will(returnValue(false));
			oneOf(builder).addMethod(with(ObjectMethodMatcher.called(null, "myname")));
		}});
		TDAObjectElementsParser parser = new TDAObjectElementsParser(errorsMock, builder, topLevel);
		TDAParsing nested = parser.tryParsing(TDABasicIntroParsingTests.line("method update"));
		assertTrue(nested instanceof TDAMethodMessageParser);
	}

	@Test
	public void objectsCanHaveUpdateMethodsWithArguments() {
		context.checking(new Expectations() {{
			allowing(errorsMock).hasErrors(); will(returnValue(false));
			oneOf(builder).addMethod(with(ObjectMethodMatcher.called(null, "myname")));
		}});
		TDAObjectElementsParser parser = new TDAObjectElementsParser(errorsMock, builder, topLevel);
		TDAParsing nested = parser.tryParsing(TDABasicIntroParsingTests.line("method update (String s)"));
		assertTrue(nested instanceof TDAMethodMessageParser);
	}

	@Test
	public void theCardCanHaveASingleTemplateDeclaration() {
		context.checking(new Expectations() {{
			allowing(errorsMock).hasErrors(); will(returnValue(false));
			oneOf(builder).addTemplate(with(any(Template.class)));
		}});
		TDAObjectElementsParser parser = new TDAObjectElementsParser(errorsMock, builder, topLevel);
		/*TDAParsing nested = */ parser.tryParsing(TDABasicIntroParsingTests.line("template my-template-name"));
//		assertTrue(nested instanceof TDAMethodMessageParser);
//		assertEquals(1, card.templates.size());
	}

	/*
	@Test
	public void theCardCanHaveMultipleTemplateDeclarations() {
		cardParser.tryParsing(TDABasicIntroParsingTests.line("template my-template-name"));
		cardParser.tryParsing(TDABasicIntroParsingTests.line("template other-template-name"));
		assertEquals(2, card.templates.size());
	}
	*/
}
