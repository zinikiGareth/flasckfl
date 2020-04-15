package test.parsing;

import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.commonBase.names.SolidName;
import org.flasck.flas.errors.ErrorMark;
import org.flasck.flas.errors.ErrorReporter;
import org.flasck.flas.parsedForm.ObjectActionHandler;
import org.flasck.flas.parsedForm.StateDefinition;
import org.flasck.flas.parsedForm.StateHolder;
import org.flasck.flas.parsedForm.Template;
import org.flasck.flas.parsedForm.TypedPattern;
import org.flasck.flas.parsedForm.VarPattern;
import org.flasck.flas.parser.IgnoreNestedParser;
import org.flasck.flas.parser.ObjectElementsConsumer;
import org.flasck.flas.parser.ObjectNestedNamer;
import org.flasck.flas.parser.TDAMethodMessageParser;
import org.flasck.flas.parser.TDAObjectElementsParser;
import org.flasck.flas.parser.TDAParsing;
import org.flasck.flas.parser.TDAStructFieldParser;
import org.flasck.flas.parser.TopLevelDefinitionConsumer;
import org.flasck.flas.stories.TDAMultiParser;
import org.flasck.flas.tc3.Type;
import org.flasck.flas.tokenizers.Tokenizable;
import org.jmock.Expectations;
import org.jmock.integration.junit4.JUnitRuleMockery;
import org.junit.Rule;
import org.junit.Test;

import flas.matchers.FunctionDefinitionMatcher;
import flas.matchers.ObjectAccessorMatcher;
import flas.matchers.ObjectCtorMatcher;
import flas.matchers.ObjectMethodMatcher;
import flas.matchers.PatternMatcher;

public class TDAObjectElementParsingTests {
	interface ObjDef extends ObjectElementsConsumer, StateHolder, Type {}
	@Rule public JUnitRuleMockery context = new JUnitRuleMockery();
	private ErrorReporter errors = context.mock(ErrorReporter.class);
	private ErrorReporter tracker = new LocalErrorTracker(errors);
	private ObjectElementsConsumer builder = context.mock(ObjDef.class);
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
		ErrorMark mark = context.mock(ErrorMark.class);
		context.checking(new Expectations() {{
			allowing(errors).hasErrors(); will(returnValue(false));
			allowing(errors).mark(); will(returnValue(mark));
			allowing(mark).hasMoreNow(); will(returnValue(false));
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
		ErrorMark mark = context.mock(ErrorMark.class);
		context.checking(new Expectations() {{
			allowing(errors).hasErrors(); will(returnValue(false));
			allowing(errors).mark(); will(returnValue(mark));
			allowing(mark).hasMoreNow(); will(returnValue(false));
			oneOf(builder).addConstructor(with(ObjectCtorMatcher.called("args").arg(PatternMatcher.var("x"))));
			oneOf(topLevel).newObjectMethod(with(any(ObjectActionHandler.class)));
			oneOf(topLevel).argument(with(any(VarPattern.class)));
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
			oneOf(topLevel).newObjectAccessor(with(ObjectAccessorMatcher.of(FunctionDefinitionMatcher.named("MyObject.myname"))));
			oneOf(builder).addAccessor(with(ObjectAccessorMatcher.of(FunctionDefinitionMatcher.named("MyObject.myname"))));
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
			oneOf(builder).addAccessor(with(ObjectAccessorMatcher.of(FunctionDefinitionMatcher.named("MyObject.myname"))));
			oneOf(topLevel).newObjectAccessor(with(ObjectAccessorMatcher.of(FunctionDefinitionMatcher.named("MyObject.myname"))));
			oneOf(topLevel).argument(with(any(VarPattern.class)));
			oneOf(topLevel).argument(with(any(TypedPattern.class)));
		}});
		TDAObjectElementsParser parser = new TDAObjectElementsParser(tracker, namer, builder, topLevel);
		TDAParsing nested = parser.tryParsing(TDABasicIntroParsingTests.line("acor myname x (Number y) = x + y"));
		parser.scopeComplete(pos);
		assertTrue(nested instanceof TDAMultiParser);
	}

	@Test
	public void anObjectCanHaveMultipleAccessorMethods() {
		context.checking(new Expectations() {{
			allowing(errors).hasErrors(); will(returnValue(false));
			oneOf(builder).addAccessor(with(ObjectAccessorMatcher.of(FunctionDefinitionMatcher.named("MyObject.myname"))));
			oneOf(topLevel).newObjectAccessor(with(ObjectAccessorMatcher.of(FunctionDefinitionMatcher.named("MyObject.myname"))));
			oneOf(builder).addAccessor(with(ObjectAccessorMatcher.of(FunctionDefinitionMatcher.named("MyObject.othername"))));
			oneOf(topLevel).newObjectAccessor(with(ObjectAccessorMatcher.of(FunctionDefinitionMatcher.named("MyObject.othername"))));
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
		ErrorMark mark = context.mock(ErrorMark.class);
		context.checking(new Expectations() {{
			allowing(errors).hasErrors(); will(returnValue(false));
			allowing(errors).mark(); will(returnValue(mark));
			allowing(mark).hasMoreNow(); will(returnValue(false));
			oneOf(builder).addMethod(with(ObjectMethodMatcher.called(objName, "update")));
			oneOf(topLevel).newObjectMethod(with(any(ObjectActionHandler.class)));
		}});
		TDAObjectElementsParser parser = new TDAObjectElementsParser(errors, namer, builder, topLevel);
		TDAParsing nested = parser.tryParsing(TDABasicIntroParsingTests.line("method update"));
		assertTrue(nested instanceof TDAMethodMessageParser);
	}

	@Test
	public void objectsCanHaveUpdateMethodsWithArguments() {
		ErrorMark mark = context.mock(ErrorMark.class);
		context.checking(new Expectations() {{
			allowing(errors).hasErrors(); will(returnValue(false));
			allowing(errors).mark(); will(returnValue(mark));
			allowing(mark).hasMoreNow(); will(returnValue(false));
			oneOf(builder).addMethod(with(ObjectMethodMatcher.called(objName, "update")));
			oneOf(topLevel).newObjectMethod(with(any(ObjectActionHandler.class)));
			oneOf(topLevel).argument(with(any(TypedPattern.class)));
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
