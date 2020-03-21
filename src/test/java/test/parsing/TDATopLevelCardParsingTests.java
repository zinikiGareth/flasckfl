package test.parsing;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.List;

import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.commonBase.Expr;
import org.flasck.flas.commonBase.names.FunctionName;
import org.flasck.flas.errors.ErrorReporter;
import org.flasck.flas.parsedForm.CardDefinition;
import org.flasck.flas.parsedForm.FunctionDefinition;
import org.flasck.flas.parsedForm.HandlerImplements;
import org.flasck.flas.parsedForm.ObjectActionHandler;
import org.flasck.flas.parsedForm.RequiresContract;
import org.flasck.flas.parsedForm.StandaloneMethod;
import org.flasck.flas.parsedForm.StateDefinition;
import org.flasck.flas.parsedForm.TypedPattern;
import org.flasck.flas.parsedForm.VarPattern;
import org.flasck.flas.parser.IgnoreNestedParser;
import org.flasck.flas.parser.PackageNamer;
import org.flasck.flas.parser.TDAIntroParser;
import org.flasck.flas.parser.TDAParsing;
import org.flasck.flas.parser.TDATemplateBindingParser;
import org.flasck.flas.parser.TopLevelDefinitionConsumer;
import org.flasck.flas.parser.TopLevelNamer;
import org.flasck.flas.stories.TDAMultiParser;
import org.flasck.flas.tc3.NamedType;
import org.flasck.flas.tokenizers.Tokenizable;
import org.jmock.Expectations;
import org.jmock.integration.junit4.JUnitRuleMockery;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.zinutils.support.jmock.CaptureAction;

import flas.matchers.CardDefnMatcher;

public class TDATopLevelCardParsingTests {
	interface CardConsumer extends NamedType, TopLevelDefinitionConsumer {};
	@Rule public JUnitRuleMockery context = new JUnitRuleMockery();
	private ErrorReporter errors = context.mock(ErrorReporter.class);
	private LocalErrorTracker tracker = new LocalErrorTracker(errors);
	private TopLevelDefinitionConsumer builder = context.mock(CardConsumer.class);
	private TopLevelNamer namer = new PackageNamer("test.pkg");
	private TDAParsing cardParser;
	private CardDefinition card;
	private InputPosition pos = new InputPosition("-", 1, 0, "hello");

	@Before
	public void setup() {
		CaptureAction captureCard = new CaptureAction(null);
		context.checking(new Expectations() {{
			allowing(errors).hasErrors(); will(returnValue(false));
			oneOf(builder).newCard(with(CardDefnMatcher.called("test.pkg.CardA"))); will(captureCard);
		}});
		TDAIntroParser intro = new TDAIntroParser(tracker, namer, builder);
		cardParser = intro.tryParsing(TDABasicIntroParsingTests.line("card CardA"));
		card = (CardDefinition) captureCard.get(0);
	}

	@Test
	public void theIntroParserCanHandleCard() {
		assertNotNull(cardParser);
		assertTrue(cardParser instanceof TDAMultiParser);
	}

	@Test
	public void aCardCanHaveAStateDeclaration() {
		assertNull(card.state);
		cardParser.tryParsing(TDABasicIntroParsingTests.line("state"));
		assertTrue(card.state instanceof StateDefinition);
	}

	@Test
	public void theCardCannotHaveTwoStateDeclarations() {
		final Tokenizable line = TDABasicIntroParsingTests.line("state");
		context.checking(new Expectations() {{
			oneOf(errors).message(line.realinfo().copySetEnd(5), "multiple state declarations");
		}});
		assertNull(card.state);
		cardParser.tryParsing(TDABasicIntroParsingTests.line("state"));
		cardParser.tryParsing(line);
	}

	@Test
	public void theCardCanHaveASingleTemplateDeclaration() {
		TDAParsing nested = cardParser.tryParsing(TDABasicIntroParsingTests.line("template my-template-name"));
		assertEquals(1, card.templates.size());
		assertTrue(nested instanceof TDATemplateBindingParser);
	}

	@Test
	public void aTemplateDeclarationMustIncludeAName() {
		Tokenizable line = TDABasicIntroParsingTests.line("template");
		context.checking(new Expectations() {{
			oneOf(errors).message(line, "template must have a name");
		}});
		TDAParsing nested = cardParser.tryParsing(line);
		assertEquals(0, card.templates.size());
		assertTrue(nested instanceof IgnoreNestedParser);
	}

	@Test
	public void theCardCanHaveMultipleTemplateDeclarations() {
		cardParser.tryParsing(TDABasicIntroParsingTests.line("template my-template-name"));
		cardParser.tryParsing(TDABasicIntroParsingTests.line("template other-template-name"));
		assertEquals(2, card.templates.size());
	}
	
	@Test
	public void cardsCanHaveEventHandlers() {
		context.checking(new Expectations() {{
			oneOf(builder).newObjectMethod(with(any(ObjectActionHandler.class)));
			oneOf(builder).argument(with(any(VarPattern.class)));
		}});
		cardParser.tryParsing(TDABasicIntroParsingTests.line("event foo ev"));
		assertEquals(1, card.eventHandlers.size());
	}

	@Test
	public void cardsCanHaveStandaloneMethods() {
		context.checking(new Expectations() {{
			oneOf(builder).newStandaloneMethod(with(any(StandaloneMethod.class)));
		}});
		cardParser.tryParsing(TDABasicIntroParsingTests.line("method m"));
	}

	@Test
	public void cardsWithStandaloneMethodsDontCascadeErrorsBecausePatternParsingIsIgnored() {
		context.checking(new Expectations() {{
			oneOf(builder).argument(with(any(TypedPattern.class)));
		}});
		// throw an error to simulate cascade
		tracker.fakeErrorWithoutNeedingAssertion();
		cardParser.tryParsing(TDABasicIntroParsingTests.line("method m (String s)"));
	}

	@Test
	public void cardsCanHaveNestedFunctions() {
		context.checking(new Expectations() {{
			oneOf(builder).functionDefn(with(any(FunctionDefinition.class)));
		}});
		TDAParsing nested = cardParser.tryParsing(TDABasicIntroParsingTests.line("f = 42"));
		nested.scopeComplete(pos);
		cardParser.scopeComplete(pos);
	}

	@Test
	public void cardsCanHaveNestedHandlers() {
		context.checking(new Expectations() {{
			oneOf(builder).newHandler(with(any(HandlerImplements.class)));
		}});
		cardParser.tryParsing(TDABasicIntroParsingTests.line("handler Contract Handler"));
	}

	@Test
	public void cardsCanProvideServicesThroughContracts() {
		cardParser.tryParsing(TDABasicIntroParsingTests.line("provides org.ziniki.ContractName"));
		assertEquals(1, card.services.size());
	}

	@Test
	public void cardsCanImplementBehaviorThroughContracts() {
		cardParser.tryParsing(TDABasicIntroParsingTests.line("implements org.ziniki.ContractName"));
		assertEquals(1, card.contracts.size());
	}

	@Test
	public void cardsCanUtilizeServicesThroughContracts() {
		context.checking(new Expectations() {{
			oneOf(builder).newRequiredContract(with(any(RequiresContract.class)));
		}});
		cardParser.tryParsing(TDABasicIntroParsingTests.line("requires org.ziniki.ContractName var"));
		assertEquals(1, card.requires.size());
	}

	@SuppressWarnings("unchecked")
	@Test
	public void cardsCanDefineNestedTuples() {
		context.checking(new Expectations() {{
			oneOf(builder).tupleDefn(with(any(List.class)), with(any(FunctionName.class)), with(any(FunctionName.class)), with(any(Expr.class)));
		}});
		cardParser.tryParsing(TDABasicIntroParsingTests.line("(x,y) = f 2"));
	}
}
