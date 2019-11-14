package test.parsing;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.List;

import org.flasck.flas.commonBase.Expr;
import org.flasck.flas.commonBase.names.FunctionName;
import org.flasck.flas.errors.ErrorReporter;
import org.flasck.flas.parsedForm.FunctionDefinition;
import org.flasck.flas.parsedForm.ServiceDefinition;
import org.flasck.flas.parsedForm.StandaloneMethod;
import org.flasck.flas.parsedForm.StateDefinition;
import org.flasck.flas.parser.PackageNamer;
import org.flasck.flas.parser.TDAIntroParser;
import org.flasck.flas.parser.TDAParsing;
import org.flasck.flas.parser.TopLevelDefinitionConsumer;
import org.flasck.flas.parser.TopLevelNamer;
import org.flasck.flas.stories.TDAMultiParser;
import org.flasck.flas.tokenizers.Tokenizable;
import org.jmock.Expectations;
import org.jmock.integration.junit4.JUnitRuleMockery;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.zinutils.support.jmock.CaptureAction;

import flas.matchers.HandlerImplementsMatcher;
import flas.matchers.ServiceDefnMatcher;

public class TDAServiceParsingTests {
	@Rule public JUnitRuleMockery context = new JUnitRuleMockery();
	private ErrorReporter errors = context.mock(ErrorReporter.class);
	private ErrorReporter tracker = new LocalErrorTracker(errors);
	private TopLevelDefinitionConsumer builder = context.mock(TopLevelDefinitionConsumer.class);
	private TopLevelNamer namer = new PackageNamer("test.pkg");
	private TDAParsing serviceParser;
	private ServiceDefinition svc;

	@Before
	public void setup() {
		CaptureAction captureCard = new CaptureAction(null);
		context.checking(new Expectations() {{
			allowing(errors).hasErrors(); will(returnValue(false));
			oneOf(builder).newService(with(ServiceDefnMatcher.called("test.pkg.ServiceA"))); will(captureCard);
		}});
		TDAIntroParser intro = new TDAIntroParser(tracker, namer, builder);
		serviceParser = intro.tryParsing(TDABasicIntroParsingTests.line("service ServiceA"));
		svc = (ServiceDefinition) captureCard.get(0);
	}

	@Test
	public void theIntroParserCanHandleService() {
		assertNotNull(serviceParser);
		assertTrue(serviceParser instanceof TDAMultiParser);
	}

	@Test
	public void aCardCanHaveAStateDeclaration() {
		assertNull(svc.state);
		serviceParser.tryParsing(TDABasicIntroParsingTests.line("state"));
		assertTrue(svc.state instanceof StateDefinition);
	}

	@Test
	public void theCardCannotHaveTwoStateDeclarations() {
		final Tokenizable line = TDABasicIntroParsingTests.line("state");
		context.checking(new Expectations() {{
			oneOf(errors).message(line.realinfo().copySetEnd(5), "multiple state declarations");
		}});
		assertNull(svc.state);
		serviceParser.tryParsing(TDABasicIntroParsingTests.line("state"));
		serviceParser.tryParsing(line);
	}

	@Test
	public void servicesCanHaveStandaloneMethods() {
		context.checking(new Expectations() {{
			oneOf(builder).newStandaloneMethod(with(any(StandaloneMethod.class)));
		}});
		serviceParser.tryParsing(TDABasicIntroParsingTests.line("method m"));
	}

	@Test
	public void servicesCanHaveNestedFunctions() {
		context.checking(new Expectations() {{
			oneOf(builder).functionDefn(with(any(FunctionDefinition.class)));
		}});
		serviceParser.tryParsing(TDABasicIntroParsingTests.line("f = 42"));
		serviceParser.scopeComplete(null);
	}

	@Test
	public void servicesCanHaveNestedHandlers() {
		context.checking(new Expectations() {{
			oneOf(builder).newHandler(with(HandlerImplementsMatcher.named("test.pkg.ServiceA.Handler")));
		}});
		serviceParser.tryParsing(TDABasicIntroParsingTests.line("handler Contract Handler"));
	}

	@Test
	public void servicesCanProvideThroughContracts() {
		context.checking(new Expectations() {{
//			oneOf(builder).newHandler(with(any(HandlerImplements.class)));
		}});
		serviceParser.tryParsing(TDABasicIntroParsingTests.line("provides org.ziniki.ContractName"));
		assertEquals(1, svc.services.size());
	}

	@SuppressWarnings("unchecked")
	@Test
	public void cardsCanDefineNestedTuples() {
		context.checking(new Expectations() {{
			oneOf(builder).tupleDefn(with(any(List.class)), with(any(FunctionName.class)), with(any(Expr.class)));
		}});
		serviceParser.tryParsing(TDABasicIntroParsingTests.line("(x,y) = f 2"));
	}
}
