package test.parsing;

import static org.junit.Assert.assertTrue;

import org.flasck.flas.errors.ErrorReporter;
import org.flasck.flas.errors.LocalErrorTracker;
import org.flasck.flas.parsedForm.HandlerImplements;
import org.flasck.flas.parsedForm.HandlerLambda;
import org.flasck.flas.parsedForm.ObjectActionHandler;
import org.flasck.flas.parsedForm.TypedPattern;
import org.flasck.flas.parsedForm.VarPattern;
import org.flasck.flas.parser.TDAIntroParser;
import org.flasck.flas.parser.PackageNamer;
import org.flasck.flas.parser.TDAImplementationMethodsParser;
import org.flasck.flas.parser.TDAParsing;
import org.flasck.flas.parser.TopLevelDefinitionConsumer;
import org.flasck.flas.parser.TopLevelNamer;
import org.jmock.Expectations;
import org.jmock.integration.junit4.JUnitRuleMockery;
import org.junit.Rule;
import org.junit.Test;

import flas.matchers.VarPatternMatcher;

public class TDAHandlerIntroParsingTests {
	@Rule public JUnitRuleMockery context = new JUnitRuleMockery();
	private ErrorReporter errors = context.mock(ErrorReporter.class);
	private LocalErrorTracker tracker = new LocalErrorTracker(errors);
	private TopLevelDefinitionConsumer builder = context.mock(TopLevelDefinitionConsumer.class);
	private TopLevelNamer namer = new PackageNamer("test.pkg");

	@Test
	public void aSimpleHandlerCanBeDefined() {
		context.checking(new Expectations() {{
			oneOf(builder).newHandler(with(tracker), with(any(HandlerImplements.class)));
		}});
		TDAIntroParser parser = new TDAIntroParser(tracker, namer, builder);
		TDAParsing nested = parser.tryParsing(TDABasicIntroParsingTests.line("handler ContractName HandlerName"));
		assertTrue(nested instanceof TDAImplementationMethodsParser);
	}

	@Test
	public void aHandlerContractNameCanHaveAQualifiedName() {
		context.checking(new Expectations() {{
			oneOf(builder).newHandler(with(tracker), with(any(HandlerImplements.class)));
		}});
		TDAIntroParser parser = new TDAIntroParser(tracker, namer, builder);
		TDAParsing nested = parser.tryParsing(TDABasicIntroParsingTests.line("handler org.ziniki.ContractName HandlerName"));
		assertTrue(nested instanceof TDAImplementationMethodsParser);
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
		TDAParsing nested = parser.tryParsing(TDABasicIntroParsingTests.line("handler org.ziniki.ContractName HandlerName x (String s)"));
		assertTrue(nested instanceof TDAImplementationMethodsParser);
	}

	@Test
	public void aHandlerCanHaveLambdaExpressionsWithPolymorphicVars() {
		context.checking(new Expectations() {{
			oneOf(builder).argument(with(tracker), with(any(TypedPattern.class)));
			oneOf(builder).replaceDefinition(with(any(HandlerLambda.class)));
			oneOf(builder).newHandler(with(tracker), with(any(HandlerImplements.class)));
		}});
		TDAIntroParser parser = new TDAIntroParser(tracker, namer, builder);
		TDAParsing nested = parser.tryParsing(TDABasicIntroParsingTests.line("handler ContractName HandlerName (List[List[Integer]] mrtho)"));
		assertTrue(nested instanceof TDAImplementationMethodsParser);
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
		TDAParsing nested = parser.tryParsing(TDABasicIntroParsingTests.line("handler ContractName HandlerName (String s) (List[List[Integer]] mrtho)"));
		assertTrue(nested instanceof TDAImplementationMethodsParser);
	}

	@Test
	public void aHandlerCanContainAMethod() {
		context.checking(new Expectations() {{
			oneOf(builder).newHandler(with(tracker), with(any(HandlerImplements.class)));
			oneOf(builder).newObjectMethod(with(tracker), with(any(ObjectActionHandler.class)));
			oneOf(builder).argument(with(tracker), (VarPattern) with(VarPatternMatcher.var("test.pkg.HandlerName.foo.x")));
		}});
		TDAIntroParser parser = new TDAIntroParser(tracker, namer, builder);
		TDAParsing nested = parser.tryParsing(TDABasicIntroParsingTests.line("handler ContractName HandlerName"));
		assertTrue(nested instanceof TDAImplementationMethodsParser);
		nested.tryParsing(TDABasicIntroParsingTests.line("foo x"));
	}


	// TODO: error cases
}
