package test.parsing;

import static org.junit.Assert.assertTrue;

import org.flasck.flas.errors.ErrorReporter;
import org.flasck.flas.parsedForm.HandlerImplements;
import org.flasck.flas.parsedForm.ObjectActionHandler;
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

public class TDAHandlerIntroParsingTests {
	@Rule public JUnitRuleMockery context = new JUnitRuleMockery();
	private ErrorReporter errors = context.mock(ErrorReporter.class);
	private LocalErrorTracker tracker = new LocalErrorTracker(errors);
	private TopLevelDefinitionConsumer builder = context.mock(TopLevelDefinitionConsumer.class);
	private TopLevelNamer namer = new PackageNamer("test.pkg");

	@Test
	public void aSimpleHandlerCanBeDefined() {
		context.checking(new Expectations() {{
			oneOf(builder).newHandler(with(any(HandlerImplements.class)));
		}});
		TDAIntroParser parser = new TDAIntroParser(errors, namer, builder);
		TDAParsing nested = parser.tryParsing(TDABasicIntroParsingTests.line("handler ContractName HandlerName"));
		assertTrue(nested instanceof TDAImplementationMethodsParser);
	}

	@Test
	public void aHandlerContractNameCanHaveAQualifiedName() {
		context.checking(new Expectations() {{
			oneOf(builder).newHandler(with(any(HandlerImplements.class)));
		}});
		TDAIntroParser parser = new TDAIntroParser(errors, namer, builder);
		TDAParsing nested = parser.tryParsing(TDABasicIntroParsingTests.line("handler org.ziniki.ContractName HandlerName"));
		assertTrue(nested instanceof TDAImplementationMethodsParser);
	}

	@Test
	public void aHandlerCanHaveLambdaExpressions() {
		context.checking(new Expectations() {{
			oneOf(builder).newHandler(with(any(HandlerImplements.class)));
			oneOf(builder).argument((VarPattern) with(VarPatternMatcher.var("test.pkg.HandlerName.x")));
		}});
		TDAIntroParser parser = new TDAIntroParser(tracker, namer, builder);
		TDAParsing nested = parser.tryParsing(TDABasicIntroParsingTests.line("handler org.ziniki.ContractName HandlerName x (String s)"));
		assertTrue(nested instanceof TDAImplementationMethodsParser);
	}

	@Test
	public void aHandlerCanHaveLambdaExpressionsWithPolymorphicVars() {
		context.checking(new Expectations() {{
			oneOf(builder).newHandler(with(any(HandlerImplements.class)));
		}});
		TDAIntroParser parser = new TDAIntroParser(tracker, namer, builder);
		TDAParsing nested = parser.tryParsing(TDABasicIntroParsingTests.line("handler ContractName HandlerName (List[List[Integer]] mrtho)"));
		assertTrue(nested instanceof TDAImplementationMethodsParser);
	}

	@Test
	public void aHandlerCanHaveMultipleLambdaExpressions() {
		context.checking(new Expectations() {{
			oneOf(builder).newHandler(with(any(HandlerImplements.class)));
		}});
		TDAIntroParser parser = new TDAIntroParser(tracker, namer, builder);
		TDAParsing nested = parser.tryParsing(TDABasicIntroParsingTests.line("handler ContractName HandlerName (String s) (List[List[Integer]] mrtho)"));
		assertTrue(nested instanceof TDAImplementationMethodsParser);
	}

	@Test
	public void aHandlerCanContainAMethod() {
		context.checking(new Expectations() {{
			oneOf(builder).newHandler(with(any(HandlerImplements.class)));
			oneOf(builder).newObjectMethod(with(any(ObjectActionHandler.class)));
			oneOf(builder).argument((VarPattern) with(VarPatternMatcher.var("test.pkg.HandlerName.foo.x")));
		}});
		TDAIntroParser parser = new TDAIntroParser(errors, namer, builder);
		TDAParsing nested = parser.tryParsing(TDABasicIntroParsingTests.line("handler ContractName HandlerName"));
		assertTrue(nested instanceof TDAImplementationMethodsParser);
		nested.tryParsing(TDABasicIntroParsingTests.line("foo x"));
	}


	// TODO: error cases
}
