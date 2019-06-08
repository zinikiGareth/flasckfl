package test.parsing;

import static org.junit.Assert.assertTrue;

import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.commonBase.names.FunctionName;
import org.flasck.flas.errors.ErrorReporter;
import org.flasck.flas.parser.FunctionNameProvider;
import org.flasck.flas.parser.IgnoreNestedParser;
import org.flasck.flas.parser.ImplementationMethodConsumer;
import org.flasck.flas.parser.TDAImplementationMethodsParser;
import org.flasck.flas.parser.TDAMethodMessageParser;
import org.flasck.flas.parser.TDAParsing;
import org.flasck.flas.parser.TopLevelDefnConsumer;
import org.flasck.flas.tokenizers.Tokenizable;
import org.jmock.Expectations;
import org.jmock.integration.junit4.JUnitRuleMockery;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

public class TDAImplementationMethodsParsingTests {
	@Rule public JUnitRuleMockery context = new JUnitRuleMockery();
	private ErrorReporter errors = context.mock(ErrorReporter.class);
	private ImplementationMethodConsumer consumer = context.mock(ImplementationMethodConsumer.class);
	private TopLevelDefnConsumer topLevel = context.mock(TopLevelDefnConsumer.class);
//	private IScope scope = context.mock(IScope.class);
//	private TDAParsing cardParser;
//	private CardDefinition card;
	private TDAImplementationMethodsParser parser;
	private FunctionNameProvider namer = context.mock(FunctionNameProvider.class);

	@Before
	public void setup() {
//		CaptureAction captureCard = new CaptureAction(null);
		context.checking(new Expectations() {{
			allowing(errors).hasErrors(); will(returnValue(false));
//			allowing(builder).scopeTo(with(any(ScopeReceiver.class))); will(new ProvideScope(scope));
//			allowing(builder).cardName("CardA"); will(returnValue(new CardName(new PackageName("A"), "CardA")));
//			oneOf(builder).newCard(with(CardDefnMatcher.called("A.CardA"))); will(captureCard);
//			oneOf(scope).define(with(errors), with("CardA"), with(any(CardDefinition.class)));
		}});
//		TDAIntroParser intro = new TDAIntroParser(errors, builder);
//		cardParser = intro.tryParsing(TDABasicIntroParsingTests.line("card CardA"));
//		card = (CardDefinition) captureCard.get(0);
		parser = new TDAImplementationMethodsParser(errors, namer, consumer, topLevel);
	}

	@Test
	public void aSingleTokenIsTheImplementationOfAMethodWithNoArgs() {
		context.checking(new Expectations() {{
			oneOf(namer).functionName(with(any(InputPosition.class)), with("foo")); will(returnValue(FunctionName.function(new InputPosition("file", 1, 10, "foo"), null, "foo")));
			oneOf(consumer).addImplementationMethod(with(ObjectMethodMatcher.called(null, "foo").withArgs(0)));
		}});
		TDAParsing nested = parser.tryParsing(TDABasicIntroParsingTests.line("foo"));
		assertTrue(nested instanceof TDAMethodMessageParser);
	}

	@Test
	public void anImplementationMayHaveASimpleArgument() {
		context.checking(new Expectations() {{
			oneOf(namer).functionName(with(any(InputPosition.class)), with("bar")); will(returnValue(FunctionName.function(new InputPosition("file", 1, 10, "bar x"), null, "bar")));
			oneOf(consumer).addImplementationMethod(with(ObjectMethodMatcher.called(null, "bar").withArgs(1)));
		}});
		TDAParsing nested = parser.tryParsing(TDABasicIntroParsingTests.line("bar x"));
		assertTrue(nested instanceof TDAMethodMessageParser);
	}

	@Test
	public void nothingHappensWhenTheImplementationsAreComplete() {
		parser.scopeComplete(new InputPosition("fred", 10, 0, "hello"));
	}

	@Test
	public void methodsCannotBeNamedForSymbols() {
		final Tokenizable line = TDABasicIntroParsingTests.line("+");
		context.checking(new Expectations() {{
			oneOf(errors).message(line, "invalid method name");
		}});
		TDAParsing nested = parser.tryParsing(line);
		assertTrue(nested instanceof IgnoreNestedParser);
	}
	
	// symbols are not valid
	// complex types are not valid
}
