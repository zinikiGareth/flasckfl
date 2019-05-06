package test.parsing;

import static org.junit.Assert.assertTrue;

import org.flasck.flas.commonBase.names.SolidName;
import org.flasck.flas.errors.ErrorReporter;
import org.flasck.flas.parsedForm.StateDefinition;
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
import org.junit.Rule;
import org.junit.Test;

public class TDAObjectElementParsingTests {
	@Rule public JUnitRuleMockery context = new JUnitRuleMockery();
	private ErrorReporter errorsMock = context.mock(ErrorReporter.class);
	private ObjectElementsConsumer builder = context.mock(ObjectElementsConsumer.class);
	
	@Test
	public void junkIsNotAKeyword() {
		context.checking(new Expectations() {{
			oneOf(errorsMock).message(with(any(Tokenizable.class)), with("'junk' is not a valid object keyword"));
		}});
		TDAObjectElementsParser parser = new TDAObjectElementsParser(errorsMock, builder);
		TDAParsing nested = parser.tryParsing(TDABasicIntroParsingTests.line("junk"));
		assertTrue(nested instanceof IgnoreNestedParser);
	}

	@Test
	public void objectsCanHaveAStateParser() {
		context.checking(new Expectations() {{
			oneOf(builder).defineState(with(any(StateDefinition.class)));
		}});
		TDAObjectElementsParser parser = new TDAObjectElementsParser(errorsMock, builder);
		TDAParsing nested = parser.tryParsing(TDABasicIntroParsingTests.line("state"));
		assertTrue(nested instanceof TDAStructFieldParser);
	}
	
	@Test
	public void objectStateCannotHaveANameOrAnything() {
		context.checking(new Expectations() {{
			oneOf(errorsMock).message(with(any(Tokenizable.class)), with("extra characters at end of line"));
		}});
		TDAObjectElementsParser parser = new TDAObjectElementsParser(errorsMock, builder);
		TDAParsing nested = parser.tryParsing(TDABasicIntroParsingTests.line("state Fred"));
		assertTrue(nested instanceof IgnoreNestedParser);
	}
	
	@Test
	public void objectsCanHaveAConstructor() {
		context.checking(new Expectations() {{
			allowing(errorsMock).hasErrors(); will(returnValue(false));
			oneOf(builder).addConstructor(with(ObjectCtorMatcher.called("simple")));
		}});
		TDAObjectElementsParser parser = new TDAObjectElementsParser(errorsMock, builder);
		TDAParsing nested = parser.tryParsing(TDABasicIntroParsingTests.line("ctor simple"));
		assertTrue(nested instanceof TDAMethodMessageParser);
	}
	
	@Test
	public void objectsCanHaveAConstructorWithAnArgument() {
		context.checking(new Expectations() {{
			allowing(errorsMock).hasErrors(); will(returnValue(false));
			oneOf(builder).addConstructor(with(ObjectCtorMatcher.called("args").arg(PatternMatcher.var("x"))));
		}});
		TDAObjectElementsParser parser = new TDAObjectElementsParser(errorsMock, builder);
		TDAParsing nested = parser.tryParsing(TDABasicIntroParsingTests.line("ctor args x"));
		assertTrue(nested instanceof TDAMethodMessageParser);
	}
	
	@Test
	public void objectsCanHaveAccessorMethods() { // Correct me if I'm wrong, but these are really functions, because they don't do state updates
		final SolidName objname = new SolidName(null, "foo");
		context.checking(new Expectations() {{
			oneOf(builder).name(); will(returnValue(objname));
			allowing(errorsMock).hasErrors(); will(returnValue(false));
			oneOf(builder).addAccessor(with(ObjectAccessorMatcher.of(FunctionCaseMatcher.called(null, "myname"))));
		}});
		TDAObjectElementsParser parser = new TDAObjectElementsParser(errorsMock, builder);
		TDAParsing nested = parser.tryParsing(TDABasicIntroParsingTests.line("acor myname = 42"));
		assertTrue(nested instanceof TDAMultiParser);
	}
}
