package test.parsing;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.flasck.flas.commonBase.names.CardName;
import org.flasck.flas.commonBase.names.PackageName;
import org.flasck.flas.compiler.ScopeReceiver;
import org.flasck.flas.errors.ErrorReporter;
import org.flasck.flas.parsedForm.CardDefinition;
import org.flasck.flas.parsedForm.IScope;
import org.flasck.flas.parsedForm.StateDefinition;
import org.flasck.flas.parser.TDACardElementsParser;
import org.flasck.flas.parser.TDAIntroParser;
import org.flasck.flas.parser.TDAParsing;
import org.flasck.flas.parser.TopLevelDefnConsumer;
import org.flasck.flas.tokenizers.Tokenizable;
import org.hamcrest.Description;
import org.jmock.Expectations;
import org.jmock.api.Action;
import org.jmock.api.Invocation;
import org.jmock.integration.junit4.JUnitRuleMockery;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.zinutils.support.jmock.CaptureAction;

public class TDATopLevelCardParsingTests {
	public static class ProvideScope implements Action {
		private IScope scope;

		public ProvideScope(IScope scope) {
			this.scope = scope;
		}

		@Override
		public void describeTo(Description arg0) {
			arg0.appendText("provide scope");
		}

		@Override
		public Object invoke(Invocation arg0) throws Throwable {
			((ScopeReceiver)arg0.getParameter(0)).provideScope(scope);
			return null;
		}

	}

	@Rule public JUnitRuleMockery context = new JUnitRuleMockery();
	private ErrorReporter errors = context.mock(ErrorReporter.class);
	private TopLevelDefnConsumer builder = context.mock(TopLevelDefnConsumer.class);
	private IScope scope = context.mock(IScope.class);
	private TDAParsing cardParser;
	private CardDefinition card;

	@Before
	public void setup() {
		CaptureAction captureCard = new CaptureAction(null);
		context.checking(new Expectations() {{
			allowing(builder).scopeTo(with(any(ScopeReceiver.class))); will(new ProvideScope(scope));
			allowing(builder).cardName("CardA"); will(returnValue(new CardName(new PackageName("A"), "CardA")));
			oneOf(builder).newCard(with(CardDefnMatcher.called("A.CardA"))); will(captureCard);
			oneOf(scope).define(with(errors), with("CardA"), with(any(CardDefinition.class)));
		}});
		TDAIntroParser intro = new TDAIntroParser(errors, builder);
		cardParser = intro.tryParsing(TDABasicIntroParsingTests.line("card CardA"));
		card = (CardDefinition) captureCard.get(0);
	}

	@Test
	public void theIntroParserCanHandleCard() {
		assertNotNull(cardParser);
		assertTrue(cardParser instanceof TDACardElementsParser);
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
		cardParser.tryParsing(TDABasicIntroParsingTests.line("template my-template-name"));
		assertEquals(1, card.templates.size());
	}

	@Test
	public void theCardCanHaveMultipleTemplateDeclarations() {
		cardParser.tryParsing(TDABasicIntroParsingTests.line("template my-template-name"));
		cardParser.tryParsing(TDABasicIntroParsingTests.line("template other-template-name"));
		assertEquals(2, card.templates.size());
	}
}
