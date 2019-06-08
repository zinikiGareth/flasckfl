package test.parsing;

import static org.junit.Assert.*;

import org.flasck.flas.commonBase.names.CardName;
import org.flasck.flas.commonBase.names.PackageName;
import org.flasck.flas.compiler.ScopeReceiver;
import org.flasck.flas.errors.ErrorReporter;
import org.flasck.flas.parsedForm.CardDefinition;
import org.flasck.flas.parsedForm.IScope;
import org.flasck.flas.parser.IgnoreNestedParser;
import org.flasck.flas.parser.LocalErrorTracker;
import org.flasck.flas.parser.TDAIntroParser;
import org.flasck.flas.parser.TDAParsing;
import org.flasck.flas.parser.TDATemplateBindingParser;
import org.flasck.flas.parser.TDATemplateOptionsParser;
import org.flasck.flas.parser.TemplateBindingConsumer;
import org.flasck.flas.parser.TopLevelDefnConsumer;
import org.flasck.flas.tokenizers.Tokenizable;
import org.jmock.Expectations;
import org.jmock.integration.junit4.JUnitRuleMockery;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.zinutils.support.jmock.CaptureAction;

import test.parsing.TDATopLevelCardParsingTests.ProvideScope;

public class TDATemplateParsingTests {
	@Rule public JUnitRuleMockery context = new JUnitRuleMockery();
	private ErrorReporter errors = context.mock(ErrorReporter.class);
	private LocalErrorTracker tracker = new LocalErrorTracker(errors);
	private TDATemplateBindingParser parser;
	private TemplateBindingConsumer consumer = context.mock(TemplateBindingConsumer.class);
//	private TopLevelDefnConsumer builder = context.mock(TopLevelDefnConsumer.class);
//	private IScope scope = context.mock(IScope.class);
//	private TDAParsing cardParser;
//	private CardDefinition card;

	@Before
	public void setup() {
		context.checking(new Expectations() {{
//			allowing(builder).scopeTo(with(any(ScopeReceiver.class))); will(new ProvideScope(scope));
//			allowing(builder).cardName("CardA"); will(returnValue(new CardName(new PackageName("A"), "CardA")));
//			oneOf(builder).newCard(with(CardDefnMatcher.called("A.CardA"))); will(captureCard);
//			oneOf(scope).define(with(errors), with("CardA"), with(any(CardDefinition.class)));
		}});
		parser = new TDATemplateBindingParser(tracker, consumer);
	}

	@Test
	public void theSimplestThingYouCanDoIsToHaveATemplateNameByItself() {
		context.checking(new Expectations() {{
			oneOf(consumer).addBinding(with(TemplateBindingMatcher.called("styling-area")));
		}});
		TDAParsing nested = parser.tryParsing(TDABasicIntroParsingTests.line("styling-area"));
		assertTrue(nested instanceof TDATemplateOptionsParser);
	}

	@Test
	public void aTemplateNameCanTakeAnExpression() {
		context.checking(new Expectations() {{
			oneOf(consumer).addBinding(with(TemplateBindingMatcher.called("styling-area")));
		}});
		TDAParsing nested = parser.tryParsing(TDABasicIntroParsingTests.line("styling-area <- member"));
		assertTrue(nested instanceof TDATemplateOptionsParser);
	}

	@Test
	public void aSimpleNameIsNotAllowedAsAnArgument() {
		final Tokenizable line = TDABasicIntroParsingTests.line("styling-area member");
		context.checking(new Expectations() {{
			oneOf(errors).message(line, "syntax error");
		}});
		TDAParsing nested = parser.tryParsing(line);
		assertTrue(nested instanceof IgnoreNestedParser);
	}

	
	// test that the degenerate case must have a scope (on scopecomplete)
}
