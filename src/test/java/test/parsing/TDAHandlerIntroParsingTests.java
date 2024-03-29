package test.parsing;

import static org.junit.Assert.assertTrue;

import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.blocker.TDAParsingWithAction;
import org.flasck.flas.commonBase.Locatable;
import org.flasck.flas.errors.ErrorReporter;
import org.flasck.flas.errors.LocalErrorTracker;
import org.flasck.flas.grammar.tracking.LoggableToken;
import org.flasck.flas.parsedForm.HandlerImplements;
import org.flasck.flas.parsedForm.HandlerLambda;
import org.flasck.flas.parsedForm.ObjectActionHandler;
import org.flasck.flas.parsedForm.TypedPattern;
import org.flasck.flas.parsedForm.VarPattern;
import org.flasck.flas.parser.PackageNamer;
import org.flasck.flas.parser.TDAImplementationMethodsParser;
import org.flasck.flas.parser.TDAIntroParser;
import org.flasck.flas.parser.TDAParsing;
import org.flasck.flas.parser.TopLevelDefinitionConsumer;
import org.flasck.flas.parser.TopLevelNamer;
import org.flasck.flas.testsupport.TestSupport;
import org.flasck.flas.testsupport.matchers.VarPatternMatcher;
import org.jmock.Expectations;
import org.jmock.integration.junit4.JUnitRuleMockery;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.zinutils.support.jmock.ReturnInvoker;

public class TDAHandlerIntroParsingTests {
	@Rule public JUnitRuleMockery context = new JUnitRuleMockery();
	private ErrorReporter errors = context.mock(ErrorReporter.class);
	private LocalErrorTracker tracker = new LocalErrorTracker(errors);
	private TopLevelDefinitionConsumer builder = context.mock(TopLevelDefinitionConsumer.class);
	private TopLevelNamer namer = new PackageNamer("test.pkg");

	@Before
	public void ignoreParserLogging() {
		context.checking(new Expectations() {{
			allowing(errors).logParsingToken(with(any(LoggableToken.class))); will(ReturnInvoker.arg(0));
			allowing(errors).logReduction(with(any(String.class)), with(any(InputPosition.class)), with(any(InputPosition.class)));
			allowing(errors).logReduction(with(any(String.class)), with(any(Locatable.class)), with(any(Locatable.class)));
		}});
	}

	@Test
	public void aSimpleHandlerCanBeDefined() {
		context.checking(new Expectations() {{
			oneOf(builder).newHandler(with(tracker), with(any(HandlerImplements.class)));
		}});
		TDAIntroParser parser = new TDAIntroParser(tracker, namer, builder);
		TDAParsing nested = parser.tryParsing(TestSupport.tokline("handler ContractName HandlerName"));
		assertTrue(nested instanceof TDAParsingWithAction);
		assertTrue(((TDAParsingWithAction)nested).parser instanceof TDAImplementationMethodsParser);
	}

	@Test
	public void aHandlerContractNameCanHaveAQualifiedName() {
		context.checking(new Expectations() {{
			oneOf(builder).newHandler(with(tracker), with(any(HandlerImplements.class)));
		}});
		TDAIntroParser parser = new TDAIntroParser(tracker, namer, builder);
		TDAParsing nested = parser.tryParsing(TestSupport.tokline("handler org.ziniki.ContractName HandlerName"));
		assertTrue(nested instanceof TDAParsingWithAction);
		assertTrue(((TDAParsingWithAction)nested).parser instanceof TDAImplementationMethodsParser);
	}

	@Test
	public void aHandlerCanHaveLambdaExpressions() {
		context.checking(new Expectations() {{
			oneOf(builder).argument(with(aNull(ErrorReporter.class)), (VarPattern) with(VarPatternMatcher.var("test.pkg.HandlerName.x")));
			oneOf(builder).argument(with(tracker), with(any(TypedPattern.class)));
			exactly(2).of(builder).replaceDefinition(with(any(HandlerLambda.class)));
			oneOf(builder).newHandler(with(tracker), with(any(HandlerImplements.class)));
		}});
		TDAIntroParser parser = new TDAIntroParser(tracker, namer, builder);
		TDAParsing nested = parser.tryParsing(TestSupport.tokline("handler org.ziniki.ContractName HandlerName x (String s)"));
		assertTrue(nested instanceof TDAParsingWithAction);
		assertTrue(((TDAParsingWithAction)nested).parser instanceof TDAImplementationMethodsParser);
	}

	@Test
	public void aHandlerCanHaveLambdaExpressionsWithPolymorphicVars() {
		context.checking(new Expectations() {{
			oneOf(builder).argument(with(tracker), with(any(TypedPattern.class)));
			oneOf(builder).replaceDefinition(with(any(HandlerLambda.class)));
			oneOf(builder).newHandler(with(tracker), with(any(HandlerImplements.class)));
		}});
		TDAIntroParser parser = new TDAIntroParser(tracker, namer, builder);
		TDAParsing nested = parser.tryParsing(TestSupport.tokline("handler ContractName HandlerName (List[List[Integer]] mrtho)"));
		assertTrue(nested instanceof TDAParsingWithAction);
		assertTrue(((TDAParsingWithAction)nested).parser instanceof TDAImplementationMethodsParser);
	}

	@Test
	public void aHandlerCanHaveMultipleLambdaExpressions() {
		context.checking(new Expectations() {{
			oneOf(builder).newHandler(with(tracker), with(any(HandlerImplements.class)));
			oneOf(builder).argument(with(tracker), with(any(TypedPattern.class)));
			exactly(2).of(builder).replaceDefinition(with(any(HandlerLambda.class)));
			oneOf(builder).argument(with(tracker), with(any(TypedPattern.class)));
		}});
		TDAIntroParser parser = new TDAIntroParser(tracker, namer, builder);
		TDAParsing nested = parser.tryParsing(TestSupport.tokline("handler ContractName HandlerName (String s) (List[List[Integer]] mrtho)"));
		assertTrue(nested instanceof TDAParsingWithAction);
		assertTrue(((TDAParsingWithAction)nested).parser instanceof TDAImplementationMethodsParser);
	}

	@Test
	public void aHandlerCanContainAMethod() {
		context.checking(new Expectations() {{
			oneOf(builder).newHandler(with(tracker), with(any(HandlerImplements.class)));
			oneOf(builder).newObjectMethod(with(tracker), with(any(ObjectActionHandler.class)));
			oneOf(builder).argument(with(tracker), (VarPattern) with(VarPatternMatcher.var("test.pkg.HandlerName.foo.x")));
		}});
		TDAIntroParser parser = new TDAIntroParser(tracker, namer, builder);
		TDAParsing nested = parser.tryParsing(TestSupport.tokline("handler ContractName HandlerName"));
		assertTrue(nested instanceof TDAParsingWithAction);
		assertTrue(((TDAParsingWithAction)nested).parser instanceof TDAImplementationMethodsParser);
		nested.tryParsing(TestSupport.tokline("foo x"));
	}


	// TODO: error cases
}
