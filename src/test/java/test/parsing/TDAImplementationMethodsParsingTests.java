package test.parsing;

import static org.junit.Assert.assertTrue;

import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.commonBase.names.FunctionName;
import org.flasck.flas.errors.ErrorReporter;
import org.flasck.flas.grammar.tracking.LoggableToken;
import org.flasck.flas.parsedForm.ObjectMethod;
import org.flasck.flas.parsedForm.VarPattern;
import org.flasck.flas.parser.FunctionNameProvider;
import org.flasck.flas.parser.IgnoreNestedParser;
import org.flasck.flas.parser.ImplementationMethodConsumer;
import org.flasck.flas.parser.TDAImplementationMethodsParser;
import org.flasck.flas.parser.TDAMethodMessageParser;
import org.flasck.flas.parser.TDAParsing;
import org.flasck.flas.parser.TopLevelDefinitionConsumer;
import org.flasck.flas.testsupport.TestSupport;
import org.flasck.flas.testsupport.matchers.ObjectMethodMatcher;
import org.flasck.flas.testsupport.matchers.VarPatternMatcher;
import org.flasck.flas.tokenizers.Tokenizable;
import org.jmock.Expectations;
import org.jmock.integration.junit4.JUnitRuleMockery;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.zinutils.support.jmock.ReturnInvoker;

public class TDAImplementationMethodsParsingTests {
	@Rule public JUnitRuleMockery context = new JUnitRuleMockery();
	private ErrorReporter errors = context.mock(ErrorReporter.class);
	private ImplementationMethodConsumer consumer = context.mock(ImplementationMethodConsumer.class);
	private TopLevelDefinitionConsumer topLevel = context.mock(TopLevelDefinitionConsumer.class);
	private TDAImplementationMethodsParser parser;
	private FunctionNameProvider namer = context.mock(FunctionNameProvider.class);

	@Before
	public void setup() {
		context.checking(new Expectations() {{
			allowing(errors).hasErrors(); will(returnValue(false));
			allowing(errors).logParsingToken(with(any(LoggableToken.class))); will(ReturnInvoker.arg(0));
			allowing(errors).logReduction(with(any(String.class)), with(any(InputPosition.class)), with(any(InputPosition.class)));
		}});
		parser = new TDAImplementationMethodsParser(errors, namer, consumer, topLevel, null, null);
	}

	@Test
	public void aSingleTokenIsTheImplementationOfAMethodWithNoArgs() {
		context.checking(new Expectations() {{
			oneOf(namer).functionName(with(any(InputPosition.class)), with("foo")); will(returnValue(FunctionName.function(new InputPosition("file", 1, 10, null, "foo"), null, "foo")));
			oneOf(consumer).addImplementationMethod(with(ObjectMethodMatcher.called(null, "foo").withArgs(0)));
			oneOf(topLevel).newObjectMethod(with(errors), with(any(ObjectMethod.class)));
		}});
		TDAParsing nested = parser.tryParsing(TestSupport.tokline("foo"));
		assertTrue(nested instanceof TDAMethodMessageParser);
	}

	@Test
	public void anImplementationMayHaveASimpleArgument() {
		final Tokenizable line = TestSupport.tokline("bar x");
		context.checking(new Expectations() {{
			oneOf(namer).functionName(with(any(InputPosition.class)), with("bar")); will(returnValue(FunctionName.function(new InputPosition("file", 1, 10, null, "bar x"), null, "bar")));
			oneOf(consumer).addImplementationMethod(with(ObjectMethodMatcher.called(null, "bar").withArgs(1)));
			oneOf(topLevel).newObjectMethod(with(errors), with(any(ObjectMethod.class)));
			oneOf(topLevel).argument(with(errors), (VarPattern) with(VarPatternMatcher.var("bar.x")));
		}});
		TDAParsing nested = parser.tryParsing(line);
		assertTrue(nested instanceof TDAMethodMessageParser);
		nested.scopeComplete(line.realinfo());
		parser.scopeComplete(line.realinfo());
	}

	@Test
	public void anImplementationMayHaveASimpleArgumentAndAHandler() {
		final Tokenizable line = TestSupport.tokline("bar x -> h");
		context.checking(new Expectations() {{
			oneOf(namer).functionName(with(any(InputPosition.class)), with("bar")); will(returnValue(FunctionName.function(new InputPosition("file", 1, 10, null, "bar x"), null, "bar")));
			oneOf(consumer).addImplementationMethod(with(ObjectMethodMatcher.called(null, "bar").withArgs(1).withHandler("bar.h")));
			oneOf(topLevel).newObjectMethod(with(errors), with(any(ObjectMethod.class)));
			oneOf(topLevel).argument(with(errors), (VarPattern) with(VarPatternMatcher.var("bar.x")));
			oneOf(topLevel).argument(with(errors), (VarPattern) with(VarPatternMatcher.var("bar.h")));
		}});
		TDAParsing nested = parser.tryParsing(line);
		assertTrue(nested instanceof TDAMethodMessageParser);
		nested.scopeComplete(line.realinfo());
		parser.scopeComplete(line.realinfo());
	}

	@Test
	public void nothingHappensWhenTheImplementationsAreComplete() {
		parser.scopeComplete(new InputPosition("fred", 10, 0, null, "hello"));
	}

	@Test
	public void methodsCannotBeNamedForSymbols() {
		final Tokenizable line = TestSupport.tokline("+");
		context.checking(new Expectations() {{
			oneOf(errors).message(line, "invalid method name");
		}});
		TDAParsing nested = parser.tryParsing(line);
		assertTrue(nested instanceof IgnoreNestedParser);
	}
	
	// TODO: symbols are not valid
	// TODO: complex types are not valid
}
