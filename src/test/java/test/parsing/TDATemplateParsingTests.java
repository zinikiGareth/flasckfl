package test.parsing;

import static org.junit.Assert.*;

import org.flasck.flas.errors.ErrorReporter;
import org.flasck.flas.parser.IgnoreNestedParser;
import org.flasck.flas.parser.LocalErrorTracker;
import org.flasck.flas.parser.TDAParsing;
import org.flasck.flas.parser.TDATemplateBindingParser;
import org.flasck.flas.parser.TDATemplateOptionsParser;
import org.flasck.flas.parser.TemplateBindingConsumer;
import org.flasck.flas.tokenizers.Tokenizable;
import org.jmock.Expectations;
import org.jmock.integration.junit4.JUnitRuleMockery;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

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
			oneOf(consumer).addBinding(with(TemplateBindingMatcher.called("styling-area").expr("member")));
		}});
		TDAParsing nested = parser.tryParsing(TDABasicIntroParsingTests.line("styling-area <- member"));
		assertTrue(nested instanceof TDATemplateOptionsParser);
	}

	@Test
	public void forTheObjectCaseABindingMustSpecifyTheObjectTemplateToUseForRendering() {
		context.checking(new Expectations() {{
			oneOf(consumer).addBinding(with(TemplateBindingMatcher.called("styling-area").expr("member").sendsTo("object-template")));
		}});
		TDAParsing nested = parser.tryParsing(TDABasicIntroParsingTests.line("styling-area <- member => object-template"));
		assertTrue(nested instanceof TDATemplateOptionsParser);
	}

	@Test
	public void aTemplateNameWithSendMustHaveAnExpression() {
		final Tokenizable line = TDABasicIntroParsingTests.line("styling-area <-");
		context.checking(new Expectations() {{
			oneOf(errors).message(line, "no expression to send");
		}});
		TDAParsing nested = parser.tryParsing(line);
		assertTrue(nested instanceof IgnoreNestedParser);
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

	@Test
	public void aTemplateNameWithSendToMustHaveDoneSendFirst() {
		final Tokenizable line = TDABasicIntroParsingTests.line("styling-area => object-template");
		context.checking(new Expectations() {{
			oneOf(errors).message(line, "missing expression");
		}});
		TDAParsing nested = parser.tryParsing(line);
		assertTrue(nested instanceof IgnoreNestedParser);
	}

	@Test
	public void aTemplateNameWithSendToMustHaveATemplateNameToSendTo() {
		final Tokenizable line = TDABasicIntroParsingTests.line("styling-area <- obj =>");
		context.checking(new Expectations() {{
			oneOf(errors).message(line, "missing template name");
		}});
		TDAParsing nested = parser.tryParsing(line);
		assertTrue(nested instanceof IgnoreNestedParser);
	}

	// => template
	// handle having or not having option binds but having customization 
	// test that the degenerate case must have customization (on scopecomplete)
}
