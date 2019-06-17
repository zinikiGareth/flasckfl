package test.parsing;

import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.commonBase.names.SolidName;
import org.flasck.flas.errors.ErrorReporter;
import org.flasck.flas.parsedForm.ObjectActionHandler;
import org.flasck.flas.parsedForm.StateDefinition;
import org.flasck.flas.parsedForm.Template;
import org.flasck.flas.parser.IgnoreNestedParser;
import org.flasck.flas.parser.ObjectElementsConsumer;
import org.flasck.flas.parser.ObjectNestedNamer;
import org.flasck.flas.parser.TDAMethodMessageParser;
import org.flasck.flas.parser.TDAObjectElementsParser;
import org.flasck.flas.parser.TDAParsing;
import org.flasck.flas.parser.TDAStructFieldParser;
import org.flasck.flas.parser.TopLevelDefinitionConsumer;
import org.flasck.flas.stories.TDAMultiParser;
import org.flasck.flas.tokenizers.Tokenizable;
import org.jmock.Expectations;
import org.jmock.integration.junit4.JUnitRuleMockery;
import org.junit.Rule;
import org.junit.Test;

public class TDAObjectElementParsingTests {
	@Rule public JUnitRuleMockery context = new JUnitRuleMockery();
	private ErrorReporter errors = context.mock(ErrorReporter.class);
	private ErrorReporter tracker = new LocalErrorTracker(errors);
	private ObjectElementsConsumer builder = context.mock(ObjectElementsConsumer.class);
	private TopLevelDefinitionConsumer topLevel = context.mock(TopLevelDefinitionConsumer.class);
	final SolidName objName = new SolidName(null, "MyObject");
	private ObjectNestedNamer namer = new ObjectNestedNamer(objName);
	private InputPosition pos = new InputPosition("-", 1, 0, "hello");

	@Test
	public void junkIsNotAKeyword() {
		TDAObjectElementsParser parser = new TDAObjectElementsParser(errors, namer, builder, topLevel);
		TDAParsing nested = parser.tryParsing(TDABasicIntroParsingTests.line("junk"));
		assertNull(nested);
	}

	@Test
	public void objectsCanHaveAStateParser() {
		context.checking(new Expectations() {{
			oneOf(builder).defineState(with(any(StateDefinition.class)));
		}});
		TDAObjectElementsParser parser = new TDAObjectElementsParser(errors, namer, builder, topLevel);
		TDAParsing nested = parser.tryParsing(TDABasicIntroParsingTests.line("state"));
		assertTrue(nested instanceof TDAStructFieldParser);
	}
	
	@Test
	public void objectStateCannotHaveANameOrAnything() {
		context.checking(new Expectations() {{
			oneOf(errors).message(with(any(Tokenizable.class)), with("extra characters at end of line"));
		}});
		TDAObjectElementsParser parser = new TDAObjectElementsParser(errors, namer, builder, topLevel);
		TDAParsing nested = parser.tryParsing(TDABasicIntroParsingTests.line("state Fred"));
		assertTrue(nested instanceof IgnoreNestedParser);
	}
	
	@Test
	public void objectsCanHaveAConstructor() {
		context.checking(new Expectations() {{
			allowing(errors).hasErrors(); will(returnValue(false));
			oneOf(builder).addConstructor(with(ObjectCtorMatcher.called("simple")));
			oneOf(topLevel).newObjectMethod(with(any(ObjectActionHandler.class)));
		}});
		TDAObjectElementsParser parser = new TDAObjectElementsParser(errors, namer, builder, topLevel);
		TDAParsing nested = parser.tryParsing(TDABasicIntroParsingTests.line("ctor simple"));
		assertTrue(nested instanceof TDAMethodMessageParser);
		nested.scopeComplete(pos);
		parser.scopeComplete(pos);
	}
	
	@Test
	public void objectsCanHaveAConstructorWithAnArgument() {
		context.checking(new Expectations() {{
			allowing(errors).hasErrors(); will(returnValue(false));
			oneOf(builder).addConstructor(with(ObjectCtorMatcher.called("args").arg(PatternMatcher.var("x"))));
			oneOf(topLevel).newObjectMethod(with(any(ObjectActionHandler.class)));
		}});
		TDAObjectElementsParser parser = new TDAObjectElementsParser(errors, namer, builder, topLevel);
		TDAParsing nested = parser.tryParsing(TDABasicIntroParsingTests.line("ctor args x"));
		assertTrue(nested instanceof TDAMethodMessageParser);
		nested.scopeComplete(pos);
		parser.scopeComplete(pos);
	}
	
	@Test
	public void objectsCanHaveAccessorMethods() { // Correct me if I'm wrong, but these are really functions, because they don't do state updates
		context.checking(new Expectations() {{
			allowing(errors).hasErrors(); will(returnValue(false));
			oneOf(topLevel).functionDefn(with(FunctionDefinitionMatcher.named("MyObject.myname")));
			oneOf(builder).addAccessor(with(ObjectAccessorMatcher.of(FunctionCaseMatcher.called(new SolidName(null, "MyObject"), "myname"))));
		}});
		TDAObjectElementsParser parser = new TDAObjectElementsParser(tracker, namer, builder, topLevel);
		TDAParsing nested = parser.tryParsing(TDABasicIntroParsingTests.line("acor myname = 42"));
		assertTrue(nested instanceof TDAMultiParser);
		nested.scopeComplete(pos);
		parser.scopeComplete(pos);
	}

	@Test
	public void objectsCanHaveAccessorMethodsWithFunctionArguments() {
		context.checking(new Expectations() {{
			allowing(errors).hasErrors(); will(returnValue(false));
			oneOf(builder).addAccessor(with(ObjectAccessorMatcher.of(FunctionCaseMatcher.called(null, "myname"))));
		}});
		TDAObjectElementsParser parser = new TDAObjectElementsParser(tracker, namer, builder, topLevel);
		TDAParsing nested = parser.tryParsing(TDABasicIntroParsingTests.line("acor myname x (Number y) = x + y"));
		assertTrue(nested instanceof TDAMultiParser);
	}

	@Test
	public void anObjectCanHaveMultipleAccessorMethods() {
		context.checking(new Expectations() {{
			allowing(errors).hasErrors(); will(returnValue(false));
			oneOf(builder).addAccessor(with(ObjectAccessorMatcher.of(FunctionCaseMatcher.called(new SolidName(null, "MyObject"), "myname"))));
			oneOf(topLevel).functionDefn(with(FunctionDefinitionMatcher.named("MyObject.myname")));
			oneOf(builder).addAccessor(with(ObjectAccessorMatcher.of(FunctionCaseMatcher.called(new SolidName(null, "MyObject"), "othername"))));
			oneOf(topLevel).functionDefn(with(FunctionDefinitionMatcher.named("MyObject.othername")));
		}});
		TDAObjectElementsParser parser = new TDAObjectElementsParser(tracker, namer, builder, topLevel);
		TDAParsing nested = parser.tryParsing(TDABasicIntroParsingTests.line("acor myname = 42"));
		assertTrue(nested instanceof TDAMultiParser);
		nested.scopeComplete(pos);
		nested = parser.tryParsing(TDABasicIntroParsingTests.line("acor othername = 76"));
		assertTrue(nested instanceof TDAMultiParser);
		nested.scopeComplete(pos);
		parser.scopeComplete(pos);
	}

	@Test
	public void objectsCanHaveUpdateMethods() {
		context.checking(new Expectations() {{
			allowing(errors).hasErrors(); will(returnValue(false));
			oneOf(builder).addMethod(with(ObjectMethodMatcher.called(objName, "update")));
			oneOf(topLevel).newObjectMethod(with(any(ObjectActionHandler.class)));
		}});
		TDAObjectElementsParser parser = new TDAObjectElementsParser(errors, namer, builder, topLevel);
		TDAParsing nested = parser.tryParsing(TDABasicIntroParsingTests.line("method update"));
		assertTrue(nested instanceof TDAMethodMessageParser);
	}

	@Test
	public void objectsCanHaveUpdateMethodsWithArguments() {
		context.checking(new Expectations() {{
			allowing(errors).hasErrors(); will(returnValue(false));
			oneOf(builder).addMethod(with(ObjectMethodMatcher.called(objName, "update")));
			oneOf(topLevel).newObjectMethod(with(any(ObjectActionHandler.class)));
		}});
		TDAObjectElementsParser parser = new TDAObjectElementsParser(errors, namer, builder, topLevel);
		TDAParsing nested = parser.tryParsing(TDABasicIntroParsingTests.line("method update (String s)"));
		assertTrue(nested instanceof TDAMethodMessageParser);
	}

	@Test
	public void objectsCanHaveASingleTemplateDeclaration() {
		context.checking(new Expectations() {{
			allowing(errors).hasErrors(); will(returnValue(false));
			oneOf(builder).addTemplate(with(any(Template.class)));
		}});
		TDAObjectElementsParser parser = new TDAObjectElementsParser(errors, namer, builder, topLevel);
		/*TDAParsing nested = */ parser.tryParsing(TDABasicIntroParsingTests.line("template my-template-name"));
//		assertTrue(nested instanceof TDAMethodMessageParser);
	}

	@Test
	public void objectsCanHaveMultipleTemplateDeclarations() {
		context.checking(new Expectations() {{
			allowing(errors).hasErrors(); will(returnValue(false));
			oneOf(builder).addTemplate(with(any(Template.class)));
			oneOf(builder).addTemplate(with(any(Template.class)));
		}});
		TDAObjectElementsParser parser = new TDAObjectElementsParser(errors, namer, builder, topLevel);
		parser.tryParsing(TDABasicIntroParsingTests.line("template my-template-name"));
		parser.tryParsing(TDABasicIntroParsingTests.line("template other-template-name"));
//		assertTrue(nested instanceof TDAMethodMessageParser);
	}

}
