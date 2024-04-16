package test.parsing;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.blocker.TDAParsingWithAction;
import org.flasck.flas.commonBase.StringLiteral;
import org.flasck.flas.commonBase.names.CardName;
import org.flasck.flas.commonBase.names.PackageName;
import org.flasck.flas.commonBase.names.TemplateName;
import org.flasck.flas.errors.ErrorReporter;
import org.flasck.flas.errors.LocalErrorTracker;
import org.flasck.flas.grammar.tracking.LoggableToken;
import org.flasck.flas.parsedForm.Template;
import org.flasck.flas.parsedForm.TemplateBinding;
import org.flasck.flas.parsedForm.TemplateBindingOption;
import org.flasck.flas.parsedForm.TemplateEvent;
import org.flasck.flas.parsedForm.TemplateStylingOption;
import org.flasck.flas.parser.IgnoreNestedParser;
import org.flasck.flas.parser.NoNestingParser;
import org.flasck.flas.parser.TDAParsing;
import org.flasck.flas.parser.TDATemplateBindingParser;
import org.flasck.flas.parser.TDATemplateOptionsParser;
import org.flasck.flas.parser.TDATemplateStylingParser;
import org.flasck.flas.parser.TemplateBindingConsumer;
import org.flasck.flas.parser.TemplateNamer;
import org.flasck.flas.testsupport.TestSupport;
import org.flasck.flas.testsupport.matchers.ExprMatcher;
import org.flasck.flas.testsupport.matchers.StringLiteralMatcher;
import org.flasck.flas.testsupport.matchers.TemplateBindingMatcher;
import org.flasck.flas.tokenizers.Tokenizable;
import org.jmock.Expectations;
import org.jmock.integration.junit4.JUnitRuleMockery;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.zinutils.support.jmock.CaptureAction;
import org.zinutils.support.jmock.ReturnInvoker;

public class TDATemplateParsingTests {
	@Rule public JUnitRuleMockery context = new JUnitRuleMockery();
	private ErrorReporter errors = context.mock(ErrorReporter.class);
	private LocalErrorTracker tracker = new LocalErrorTracker(errors);
	private TDATemplateBindingParser parser;
	private TemplateBindingConsumer consumer = context.mock(TemplateBindingConsumer.class);
	private PackageName pkg = new PackageName("test.golden");
	private InputPosition pos = new InputPosition("fred", 10, 0, null, "hello");
	private TemplateNamer namer = context.mock(TemplateNamer.class);

	@Before
	public void setup() {
		context.checking(new Expectations() {{
			allowing(errors).logParsingToken(with(any(LoggableToken.class))); will(ReturnInvoker.arg(0));
			allowing(errors).logReduction(with(any(String.class)), with(any(InputPosition.class)), with(any(InputPosition.class)));
		}});
		Template source = new Template(pos, pos, null, 0, null);
		parser = new TDATemplateBindingParser(tracker, source, namer, consumer, null);
	}

	@Test
	public void theSimplestThingYouCanDoIsToHaveATemplateNameByItself() {
		context.checking(new Expectations() {{
			oneOf(consumer).addBinding(with(TemplateBindingMatcher.called("styling-area")));
		}});
		TDAParsing nested = parser.tryParsing(TestSupport.tokline("styling-area"));
		assertTrue(TDAParsingWithAction.is(nested, TDATemplateOptionsParser.class));
		parser.scopeComplete(pos);
	}

	@Test
	public void aTemplateNameCanTakeAnExpression() {
		context.checking(new Expectations() {{
			oneOf(consumer).addBinding(with(TemplateBindingMatcher.called("styling-area").expr("member")));
		}});
		TDAParsing nested = parser.tryParsing(TestSupport.tokline("styling-area <- member"));
		assertTrue(TDAParsingWithAction.is(nested, TDATemplateOptionsParser.class));
	}

	@Test
	public void forTheObjectCaseABindingMustSpecifyTheObjectTemplateToUseForRendering() {
		TemplateName ot = new TemplateName(pos, new CardName(pkg, "Card"), "object-template");
		context.checking(new Expectations() {{
			oneOf(namer).template(with(any(InputPosition.class)), with("object-template")); will(returnValue(ot));
			oneOf(consumer).addBinding(with(TemplateBindingMatcher.called("styling-area").expr("member").sendsTo(ot)));
		}});
		TDAParsing nested = parser.tryParsing(TestSupport.tokline("styling-area <- member => object-template"));
		assertTrue(TDAParsingWithAction.is(nested, TDATemplateOptionsParser.class));
	}

	@Test
	public void aTemplateNameWithSendMustHaveAnExpression() {
		final Tokenizable line = TestSupport.tokline("styling-area <-");
		context.checking(new Expectations() {{
			oneOf(errors).message(line, "no expression to send");
		}});
		TDAParsing nested = parser.tryParsing(line);
		assertTrue(nested instanceof IgnoreNestedParser);
	}

	@Test
	public void aSimpleNameIsNotAllowedAsAnArgument() {
		final Tokenizable line = TestSupport.tokline("styling-area member");
		context.checking(new Expectations() {{
			oneOf(errors).message(line, "syntax error");
		}});
		TDAParsing nested = parser.tryParsing(line);
		assertTrue(nested instanceof IgnoreNestedParser);
	}

	@Test
	public void aTemplateNameWithSendToMustHaveDoneSendFirst() {
		final Tokenizable line = TestSupport.tokline("styling-area => object-template");
		context.checking(new Expectations() {{
			oneOf(errors).message(line, "missing expression");
		}});
		TDAParsing nested = parser.tryParsing(line);
		assertTrue(nested instanceof IgnoreNestedParser);
	}

	@Test
	public void aTemplateNameWithSendToMustHaveATemplateNameToSendTo() {
		final Tokenizable line = TestSupport.tokline("styling-area <- obj =>");
		context.checking(new Expectations() {{
			oneOf(errors).message(line, "missing template name");
		}});
		TDAParsing nested = parser.tryParsing(line);
		assertTrue(nested instanceof IgnoreNestedParser);
	}

	@Test
	public void aNameByItselfMayHaveADefaultSendOptionWithoutSendTo() {
		CaptureAction captureIt = new CaptureAction(null);
		context.checking(new Expectations() {{
			oneOf(consumer).addBinding(with(TemplateBindingMatcher.called("styling-area"))); will(captureIt);
		}});
		TDAParsing nested = parser.tryParsing(TestSupport.tokline("styling-area"));
		TDAParsing styling = nested.tryParsing(TestSupport.tokline("<- 'hello'"));
		assertTrue(TDAParsingWithAction.is(nested, TDATemplateOptionsParser.class));
		styling.scopeComplete(pos);
		nested.scopeComplete(pos);
		final TemplateBindingOption db = ((TemplateBinding)captureIt.get(0)).defaultBinding;
		assertNotNull(db);
		assertNull(db.cond);
		assertTrue(new StringLiteralMatcher("hello").matches(db.expr));
	}

	@Test
	public void forTheObjectCaseADefaultExpressionMayHaveATemplate() {
		CaptureAction captureIt = new CaptureAction(null);
		TemplateName tn = new TemplateName(pos, new CardName(pkg, "Card"), "template-7");
		context.checking(new Expectations() {{
			oneOf(namer).template(with(any(InputPosition.class)), with("template-7")); will(returnValue(tn));
			oneOf(consumer).addBinding(with(TemplateBindingMatcher.called("styling-area"))); will(captureIt);
		}});
		TDAParsing nested = parser.tryParsing(TestSupport.tokline("styling-area"));
		nested.tryParsing(TestSupport.tokline("<- obj => template-7"));
		nested.scopeComplete(pos);
		final TemplateBindingOption db = ((TemplateBinding)captureIt.get(0)).defaultBinding;
		assertNotNull(db);
		assertNull(db.cond);
		assertThat(db.expr, ExprMatcher.unresolved("obj"));
	}

	@Test
	public void aNameByItselfMayHaveAConditionalSendOptionWithoutSendTo() {
		CaptureAction captureIt = new CaptureAction(null);
		context.checking(new Expectations() {{
			oneOf(consumer).addBinding(with(TemplateBindingMatcher.called("styling-area"))); will(captureIt);
		}});
		TDAParsing nested = parser.tryParsing(TestSupport.tokline("styling-area"));
		nested.tryParsing(TestSupport.tokline("| true <- 'hello'"));
		nested.scopeComplete(pos);
		final TemplateBinding binding = (TemplateBinding)captureIt.get(0);
		assertEquals(1, binding.conditionalBindings.size());
		assertNull(binding.defaultBinding);
		TemplateBindingOption db = binding.conditionalBindings.get(0);
		assertThat(db.cond, ExprMatcher.unresolved("true"));
		assertThat((StringLiteral)db.expr, new StringLiteralMatcher("hello"));
		assertNull(db.sendsTo);
	}

	@Test
	public void aNameByItselfMayHaveAConditionalSendOptionWithSendTo() {
		CaptureAction captureIt = new CaptureAction(null);
		TemplateName tn = new TemplateName(pos, new CardName(pkg, "Card"), "my-template");
		context.checking(new Expectations() {{
			oneOf(consumer).addBinding(with(TemplateBindingMatcher.called("styling-area"))); will(captureIt);
			oneOf(namer).template(with(any(InputPosition.class)), with("my-template")); will(returnValue(tn));
		}});
		TDAParsing nested = parser.tryParsing(TestSupport.tokline("styling-area"));
		nested.tryParsing(TestSupport.tokline("| true <- obj => my-template"));
		nested.scopeComplete(pos);
		final TemplateBinding binding = (TemplateBinding)captureIt.get(0);
		assertEquals(1, binding.conditionalBindings.size());
		assertNull(binding.defaultBinding);
		TemplateBindingOption db = binding.conditionalBindings.get(0);
		assertThat(db.cond, ExprMatcher.unresolved("true"));
		assertThat(db.expr, ExprMatcher.unresolved("obj"));
		assertEquals("my-template", db.sendsTo.name.baseName());
	}

	@Test
	public void aNameByItselfMayHaveMultipleConditionals() {
		CaptureAction captureIt = new CaptureAction(null);
		context.checking(new Expectations() {{
			oneOf(consumer).addBinding(with(TemplateBindingMatcher.called("styling-area"))); will(captureIt);
		}});
		TDAParsing nested = parser.tryParsing(TestSupport.tokline("styling-area"));
		nested.tryParsing(TestSupport.tokline("| true <- 'hello'"));
		nested.tryParsing(TestSupport.tokline("| false <- 42"));
		nested.scopeComplete(pos);
		final TemplateBinding binding = (TemplateBinding)captureIt.get(0);
		assertEquals(2, binding.conditionalBindings.size());
		assertNull(binding.defaultBinding);
	}

	@Test
	public void aNameByItselfMayHaveMultipleConditionalsAndADefault() {
		CaptureAction captureIt = new CaptureAction(null);
		context.checking(new Expectations() {{
			oneOf(consumer).addBinding(with(TemplateBindingMatcher.called("styling-area"))); will(captureIt);
		}});
		TDAParsing nested = parser.tryParsing(TestSupport.tokline("styling-area"));
		nested.tryParsing(TestSupport.tokline("| true <- 'hello'"));
		nested.tryParsing(TestSupport.tokline("| false <- 42"));
		nested.tryParsing(TestSupport.tokline("<- 86"));
		nested.scopeComplete(pos);
		final TemplateBinding binding = (TemplateBinding)captureIt.get(0);
		assertEquals(2, binding.conditionalBindings.size());
		assertNotNull(binding.defaultBinding);
	}

	@Test
	public void aNameByItselfMayHaveAConditionalStyling() {
		CaptureAction captureIt = new CaptureAction(null);
		context.checking(new Expectations() {{
			oneOf(consumer).addBinding(with(TemplateBindingMatcher.called("styling-area"))); will(captureIt);
		}});
		TDAParsing nested = parser.tryParsing(TestSupport.tokline("styling-area"));
		TDAParsing styling = nested.tryParsing(TestSupport.tokline("| true => 'style1'"));
		assertTrue(TDAParsingWithAction.is(styling, TDATemplateStylingParser.class));
		styling.scopeComplete(pos);
		nested.scopeComplete(pos);
		final TemplateBinding binding = (TemplateBinding)captureIt.get(0);
		assertEquals(1, binding.conditionalStylings.size());
		TemplateStylingOption db = binding.conditionalStylings.get(0);
		assertThat(db.cond, ExprMatcher.unresolved("true"));
		assertEquals(1, db.styles.size());
		assertThat((StringLiteral)db.styles.get(0), new StringLiteralMatcher("style1"));
	}

	@Test
	public void aDefaultBindingMayHaveAConditionalStyling() {
		CaptureAction captureIt = new CaptureAction(null);
		context.checking(new Expectations() {{
			oneOf(consumer).addBinding(with(TemplateBindingMatcher.called("styling-area"))); will(captureIt);
		}});
		TDAParsing nested = parser.tryParsing(TestSupport.tokline("styling-area"));
		TDAParsing styling = nested.tryParsing(TestSupport.tokline("<- 'hello'"));
		TDAParsing nestedStyling = styling.tryParsing(TestSupport.tokline("| true => 'style1'"));
		assertTrue(TDAParsingWithAction.is(nestedStyling, TDATemplateStylingParser.class));
		styling.scopeComplete(pos);
		nested.scopeComplete(pos);
		final TemplateBinding binding = (TemplateBinding)captureIt.get(0);
		assertEquals(1, binding.defaultBinding.conditionalStylings.size());
		TemplateStylingOption db = binding.defaultBinding.conditionalStylings.get(0);
		assertThat(db.cond, ExprMatcher.unresolved("true"));
		assertEquals(1, db.styles.size());
		assertThat((StringLiteral)db.styles.get(0), new StringLiteralMatcher("style1"));
	}

	@Test
	public void aNameByItselfMayHaveAnEventHandler() {
		CaptureAction captureIt = new CaptureAction(null);
		context.checking(new Expectations() {{
			oneOf(consumer).addBinding(with(TemplateBindingMatcher.called("styling-area"))); will(captureIt);
		}});
		TDAParsing nested = parser.tryParsing(TestSupport.tokline("styling-area"));
		nested.tryParsing(TestSupport.tokline("=> handle"));
		nested.scopeComplete(pos);
		final TemplateBinding binding = (TemplateBinding)captureIt.get(0);
		assertEquals(1, binding.events.size());
		TemplateEvent db = binding.events.get(0);
		assertEquals("handle", db.handler);
	}

	@Test
	public void aDefaultBindingMayHaveAnEventHandler() {
		CaptureAction captureIt = new CaptureAction(null);
		context.checking(new Expectations() {{
			oneOf(consumer).addBinding(with(TemplateBindingMatcher.called("styling-area"))); will(captureIt);
		}});
		TDAParsing nested = parser.tryParsing(TestSupport.tokline("styling-area"));
		TDAParsing styling = nested.tryParsing(TestSupport.tokline("<- 'hello'"));
		TDAParsing nomore = styling.tryParsing(TestSupport.tokline("=> handle"));
		assertTrue(nomore instanceof NoNestingParser);
		styling.scopeComplete(pos);
		nested.scopeComplete(pos);
		final TemplateBinding binding = (TemplateBinding)captureIt.get(0);
		assertEquals(1, binding.defaultBinding.events.size());
		TemplateEvent db = binding.defaultBinding.events.get(0);
		assertEquals("handle", db.handler);
	}

	@Test
	public void aSimpleBindingMayHaveAConditionalStyle() {
		CaptureAction captureIt = new CaptureAction(null);
		context.checking(new Expectations() {{
			oneOf(consumer).addBinding(with(TemplateBindingMatcher.called("mmhhezj").expr("(- true eg)"))); will(captureIt);
		}});
		TDAParsing styling = parser.tryParsing(TestSupport.tokline("mmhhezj <- true - eg"));
		TDAParsing nested = styling.tryParsing(TestSupport.tokline("| 'bsCy+/n5r7Rh-VjPK' => 'yhbLy_?e.7<sn'"));
		assertTrue(TDAParsingWithAction.is(nested, TDATemplateStylingParser.class));
		styling.scopeComplete(pos);
		final TemplateBinding binding = (TemplateBinding)captureIt.get(0);
		assertEquals(0, binding.defaultBinding.events.size());
		assertEquals(1, binding.defaultBinding.conditionalStylings.size());
		TemplateStylingOption db = binding.defaultBinding.conditionalStylings.get(0);
		assertNotNull(db);
		assertEquals("bsCy+/n5r7Rh-VjPK", ((StringLiteral)db.cond).text);
		assertEquals(1, db.styles.size());
		assertEquals("yhbLy_?e.7<sn", ((StringLiteral)db.styles.get(0)).text);
	}

	@Test
	public void aNameByItselfMustHaveSomeNestedContent() {
		Tokenizable line = TestSupport.tokline("styling-area");
		InputPosition ep = line.realinfo().copySetEnd(12);
		context.checking(new Expectations() {{
			oneOf(consumer).addBinding(with(TemplateBindingMatcher.called("styling-area")));
			oneOf(errors).message(ep, "simple template name must have options or customization");
		}});
		TDAParsing nested = parser.tryParsing(line);
		nested.scopeComplete(pos);
	}

	@Test
	public void multipleDefaultBindingsAreNotPermitted() {
		CaptureAction captureIt = new CaptureAction(null);
		final Tokenizable errline = TestSupport.tokline("<- 42");
		context.checking(new Expectations() {{
			oneOf(consumer).addBinding(with(TemplateBindingMatcher.called("styling-area"))); will(captureIt);
			oneOf(errors).message(errline, "multiple default bindings are not permitted");
		}});
		TDAParsing nested = parser.tryParsing(TestSupport.tokline("styling-area"));
		nested.tryParsing(TestSupport.tokline("<- 'hello'"));
		nested.tryParsing(errline);
		nested.scopeComplete(pos);
		assertNotNull(((TemplateBinding)captureIt.get(0)).defaultBinding);
	}

	@Test
	public void defaultBindingsCannotHaveJunkAtTheEnd() {
		CaptureAction captureIt = new CaptureAction(null);
		final Tokenizable errline = TestSupport.tokline("<- 42 =");
		context.checking(new Expectations() {{
			oneOf(consumer).addBinding(with(TemplateBindingMatcher.called("styling-area"))); will(captureIt);
			oneOf(errors).message(errline, "syntax error");
		}});
		TDAParsing nested = parser.tryParsing(TestSupport.tokline("styling-area"));
		nested.tryParsing(errline);
		nested.scopeComplete(pos);
	}

	@Test
	public void aTemplateBindingOnOneLineCannotHaveNestedOptions() {
		CaptureAction captureIt = new CaptureAction(null);
		final Tokenizable errline = TestSupport.tokline("<- 42");
		TemplateName ot = new TemplateName(pos, new CardName(pkg, "Card"), "object-template");
		context.checking(new Expectations() {{
			oneOf(namer).template(with(any(InputPosition.class)), with("object-template")); will(returnValue(ot));
			oneOf(consumer).addBinding(with(TemplateBindingMatcher.called("styling-area").expr("member").sendsTo(ot))); will(captureIt);
			oneOf(errors).message(errline, "syntax error");
		}});
		TDAParsing nested = parser.tryParsing(TestSupport.tokline("styling-area <- member => object-template"));
		nested.tryParsing(errline);
		nested.scopeComplete(pos);
	}

	@Test
	public void aTemplateBindingOnOneLineCannotHaveNestedConditionals() {
		CaptureAction captureIt = new CaptureAction(null);
		final Tokenizable errline = TestSupport.tokline("| true <- 42");
		TemplateName ot = new TemplateName(pos, new CardName(pkg, "Card"), "object-template");
		context.checking(new Expectations() {{
			oneOf(namer).template(with(any(InputPosition.class)), with("object-template")); will(returnValue(ot));
			oneOf(consumer).addBinding(with(TemplateBindingMatcher.called("styling-area").expr("member").sendsTo(ot))); will(captureIt);
			oneOf(errors).message(errline, "conditional bindings are not permitted after the default has been specified");
		}});
		TDAParsing nested = parser.tryParsing(TestSupport.tokline("styling-area <- member => object-template"));
		nested.tryParsing(errline);
		nested.scopeComplete(pos);
	}

	@Test
	public void aTemplateBindingCannotHaveNestedConditionalsAfterANestedDefault() {
		CaptureAction captureIt = new CaptureAction(null);
		final Tokenizable errline = TestSupport.tokline("| true <- 42");
		context.checking(new Expectations() {{
			oneOf(consumer).addBinding(with(TemplateBindingMatcher.called("styling-area"))); will(captureIt);
			oneOf(errors).message(errline, "conditional bindings are not permitted after the default has been specified");
		}});
		TDAParsing nested = parser.tryParsing(TestSupport.tokline("styling-area"));
		nested.tryParsing(TestSupport.tokline("<- 86"));
		nested.tryParsing(errline);
		nested.scopeComplete(pos);
	}

	@Test
	public void cannotMixBindingsAndCustomization() {
		CaptureAction captureIt = new CaptureAction(null);
		final Tokenizable errline = TestSupport.tokline("| true => 'style1'");
		context.checking(new Expectations() {{
			oneOf(consumer).addBinding(with(TemplateBindingMatcher.called("styling-area"))); will(captureIt);
			oneOf(errors).message(with(any(InputPosition.class)), with("cannot mix bindings and customization"));
		}});
		TDAParsing nested = parser.tryParsing(TestSupport.tokline("styling-area"));
		nested.tryParsing(TestSupport.tokline("<- 86"));
		nested.tryParsing(errline);
		nested.scopeComplete(pos);
	}

	@Test
	public void cannotHaveRandomNestedLines() {
		CaptureAction captureIt = new CaptureAction(null);
		final Tokenizable errline = TestSupport.tokline("= 91");
		context.checking(new Expectations() {{
			oneOf(consumer).addBinding(with(TemplateBindingMatcher.called("styling-area"))); will(captureIt);
			oneOf(errors).message(errline, "syntax error");
		}});
		TDAParsing nested = parser.tryParsing(TestSupport.tokline("styling-area"));
		nested.tryParsing(errline);
		nested.scopeComplete(pos);
	}
}
