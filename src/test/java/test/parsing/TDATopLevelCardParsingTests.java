package test.parsing;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.net.URI;
import java.util.List;

import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.blocker.TDAParsingWithAction;
import org.flasck.flas.commonBase.Expr;
import org.flasck.flas.commonBase.names.FunctionName;
import org.flasck.flas.errors.ErrorReporter;
import org.flasck.flas.errors.LocalErrorTracker;
import org.flasck.flas.grammar.tracking.LoggableToken;
import org.flasck.flas.parsedForm.CardDefinition;
import org.flasck.flas.parsedForm.FunctionDefinition;
import org.flasck.flas.parsedForm.HandlerImplements;
import org.flasck.flas.parsedForm.ImplementsContract;
import org.flasck.flas.parsedForm.ObjectActionHandler;
import org.flasck.flas.parsedForm.RequiresContract;
import org.flasck.flas.parsedForm.StandaloneMethod;
import org.flasck.flas.parsedForm.StateDefinition;
import org.flasck.flas.parsedForm.Template;
import org.flasck.flas.parsedForm.TypedPattern;
import org.flasck.flas.parser.IgnoreNestedParser;
import org.flasck.flas.parser.PackageNamer;
import org.flasck.flas.parser.TDAIntroParser;
import org.flasck.flas.parser.TDAParsing;
import org.flasck.flas.parser.TDATemplateBindingParser;
import org.flasck.flas.parser.TopLevelDefinitionConsumer;
import org.flasck.flas.parser.TopLevelNamer;
import org.flasck.flas.stories.TDAMultiParser;
import org.flasck.flas.tc3.NamedType;
import org.flasck.flas.testsupport.TestSupport;
import org.flasck.flas.testsupport.matchers.CardDefnMatcher;
import org.flasck.flas.tokenizers.Tokenizable;
import org.jmock.Expectations;
import org.jmock.integration.junit4.JUnitRuleMockery;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.zinutils.support.jmock.CaptureAction;
import org.zinutils.support.jmock.ReturnInvoker;

public class TDATopLevelCardParsingTests {
	interface CardConsumer extends NamedType, TopLevelDefinitionConsumer {};
	@Rule public JUnitRuleMockery context = new JUnitRuleMockery();
	private ErrorReporter errors = context.mock(ErrorReporter.class);
	private LocalErrorTracker tracker = new LocalErrorTracker(errors);
	private TopLevelDefinitionConsumer builder = context.mock(CardConsumer.class);
	private TopLevelNamer namer = new PackageNamer("test.pkg");
	private TDAParsing cardParser;
	private CardDefinition card;
	private URI fred = URI.create("file:/fred");
	private InputPosition pos = new InputPosition(fred, 1, 0, null, null);

	@Before
	public void setup() {
		CaptureAction captureCard = new CaptureAction(null);
		context.checking(new Expectations() {{
			allowing(errors).hasErrors(); will(returnValue(false));
			oneOf(builder).newCard(with(tracker), with(CardDefnMatcher.called("test.pkg.CardA"))); will(captureCard);
			allowing(errors).logParsingToken(with(any(LoggableToken.class))); will(ReturnInvoker.arg(0));
			allowing(errors).logReduction(with(any(String.class)), with(any(InputPosition.class)), with(any(InputPosition.class)));
		}});
		TDAIntroParser intro = new TDAIntroParser(tracker, namer, builder);
		cardParser = intro.tryParsing(TestSupport.tokline("card CardA"));
		card = (CardDefinition) captureCard.get(1);
	}

	@Test
	public void theIntroParserCanHandleCard() {
		assertNotNull(cardParser);
		assertTrue(TDAParsingWithAction.is(cardParser, TDAMultiParser.class));
	}

	@Test
	public void aCardCanHaveAStateDeclaration() {
		assertNull(card.state);
		cardParser.tryParsing(TestSupport.tokline("state"));
		assertTrue(card.state instanceof StateDefinition);
	}

	@Test
	public void theCardCannotHaveTwoStateDeclarations() {
		final Tokenizable line = TestSupport.tokline("state");
		context.checking(new Expectations() {{
			oneOf(errors).message(line.realinfo().copySetEnd(5), "multiple state declarations");
		}});
		assertNull(card.state);
		cardParser.tryParsing(TestSupport.tokline("state"));
		cardParser.tryParsing(line);
	}

	@Test
	public void theCardCanHaveASingleTemplateDeclaration() {
		context.checking(new Expectations() {{
			oneOf(builder).newTemplate(with(tracker), with(any(Template.class)));
		}});
		TDAParsing nested = cardParser.tryParsing(TestSupport.tokline("template my-template-name"));
		assertEquals(1, card.templates.size());
		assertTrue(TDAParsingWithAction.is(nested, TDATemplateBindingParser.class));
	}

	@Test
	public void aTemplateDeclarationMustIncludeAName() {
		Tokenizable line = TestSupport.tokline("template");
		context.checking(new Expectations() {{
			oneOf(errors).message(line, "template must have a name");
		}});
		TDAParsing nested = cardParser.tryParsing(line);
		assertEquals(0, card.templates.size());
		assertTrue(nested instanceof IgnoreNestedParser);
	}

	@Test
	public void theCardCanHaveMultipleTemplateDeclarations() {
		context.checking(new Expectations() {{
			exactly(2).of(builder).newTemplate(with(tracker), with(any(Template.class)));
		}});
		cardParser.tryParsing(TestSupport.tokline("template my-template-name"));
		cardParser.tryParsing(TestSupport.tokline("template other-template-name"));
		assertEquals(2, card.templates.size());
	}
	
	@Test
	public void cardsCanHaveEventHandlers() {
		context.checking(new Expectations() {{
			oneOf(builder).newObjectMethod(with(tracker), with(any(ObjectActionHandler.class)));
			oneOf(builder).argument(with(tracker), with(any(TypedPattern.class)));
		}});
		cardParser.tryParsing(TestSupport.tokline("event foo (ClickEvent ev)"));
		assertEquals(1, card.eventHandlers.size());
	}

	@Test
	public void cardsCanHaveStandaloneMethods() {
		context.checking(new Expectations() {{
			oneOf(builder).newStandaloneMethod(with(tracker), with(any(StandaloneMethod.class)));
		}});
		cardParser.tryParsing(TestSupport.tokline("method m"));
	}

	@Test
	public void cardsWithStandaloneMethodsDontCascadeErrorsBecausePatternParsingIsIgnored() {
		context.checking(new Expectations() {{
			oneOf(builder).argument(with(tracker), with(any(TypedPattern.class)));
			oneOf(builder).newStandaloneMethod(with(tracker), with(any(StandaloneMethod.class)));
		}});
		// throw an error to simulate cascade
		tracker.fakeErrorWithoutNeedingAssertion();
		cardParser.tryParsing(TestSupport.tokline("method m (String s)"));
	}

	@Test
	public void cardsCanHaveNestedFunctions() {
		context.checking(new Expectations() {{
			oneOf(builder).functionDefn(with(tracker), with(any(FunctionDefinition.class)));
		}});
		TDAParsing nested = cardParser.tryParsing(TestSupport.tokline("f = 42"));
		nested.scopeComplete(pos);
		cardParser.scopeComplete(pos);
	}

	@Test
	public void cardsCanHaveNestedHandlers() {
		context.checking(new Expectations() {{
			oneOf(builder).newHandler(with(tracker), with(any(HandlerImplements.class)));
		}});
		cardParser.tryParsing(TestSupport.tokline("handler Contract Handler"));
		assertEquals(1, card.handlers.size());
	}

	@Test
	public void cardsCanProvideServicesThroughContracts() {
		cardParser.tryParsing(TestSupport.tokline("provides org.ziniki.ContractName"));
		assertEquals(1, card.services.size());
	}

	@Test
	public void cardsCanImplementBehaviorThroughContracts() {
		context.checking(new Expectations() {{
			oneOf(builder).newContractImpl(with(tracker), with(any(ImplementsContract.class)));
		}});
		cardParser.tryParsing(TestSupport.tokline("implements org.ziniki.ContractName"));
		assertEquals(1, card.contracts.size());
	}

	@Test
	public void cardsCanUtilizeServicesThroughContracts() {
		context.checking(new Expectations() {{
			oneOf(builder).newRequiredContract(with(tracker), with(any(RequiresContract.class)));
		}});
		cardParser.tryParsing(TestSupport.tokline("requires org.ziniki.ContractName var"));
		assertEquals(1, card.requires.size());
	}

	@SuppressWarnings("unchecked")
	@Test
	public void cardsCanDefineNestedTuples() {
		context.checking(new Expectations() {{
			oneOf(builder).tupleDefn(with(tracker), with(any(List.class)), with(any(FunctionName.class)), with(any(FunctionName.class)), with(any(Expr.class)));
		}});
		cardParser.tryParsing(TestSupport.tokline("(x,y) = f 2"));
	}
}
