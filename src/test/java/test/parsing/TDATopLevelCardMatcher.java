package test.parsing;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.flasck.flas.commonBase.names.CardName;
import org.flasck.flas.commonBase.names.PackageName;
import org.flasck.flas.compiler.ScopeReceiver;
import org.flasck.flas.errors.ErrorReporter;
import org.flasck.flas.parsedForm.CardDefinition;
import org.flasck.flas.parsedForm.IScope;
import org.flasck.flas.parser.TDACardElementsParser;
import org.flasck.flas.parser.TDAIntroParser;
import org.flasck.flas.parser.TDAParsing;
import org.flasck.flas.parser.TopLevelDefnConsumer;
import org.hamcrest.Description;
import org.jmock.Expectations;
import org.jmock.api.Action;
import org.jmock.api.Invocation;
import org.jmock.integration.junit4.JUnitRuleMockery;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

// TODO: The next phase of going TDA is to port all the "card" elements
// I think this test should just test the basic structure of that and that we
// can call "card" and get the right multi back
// It might also check some flows at the top level (e.g. can't have state twice)
// NB: ***** We want to move away from templates as we currently have them and just use webzip, so don't duplicate all that template logic *****
//     ***** See FlasZin notes for a little more info *****
public class TDATopLevelCardMatcher {
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

	@Before
	public void setup() {
		context.checking(new Expectations() {{
			allowing(builder).scopeTo(with(any(ScopeReceiver.class))); will(new ProvideScope(scope));
		}});
	}

	@Test
	public void theIntroParserCanHandleCard() {
		context.checking(new Expectations() {{
			allowing(builder).cardName("CardA"); will(returnValue(new CardName(new PackageName("A"), "CardA")));
			oneOf(builder).newCard(with(CardDefnMatcher.called("A.CardA")));
			oneOf(scope).define(with(errors), with("CardA"), with(any(CardDefinition.class)));
		}});
		TDAIntroParser parser = new TDAIntroParser(errors, builder);
		TDAParsing nested = parser.tryParsing(TDABasicIntroParsingTests.line("card CardA"));
		assertNotNull(nested);
		assertTrue(nested instanceof TDACardElementsParser);
	}
}
