package org.flasck.flas.parser;

import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.errors.ErrorReporter;
import org.flasck.flas.parsedForm.Template;
import org.flasck.flas.parsedForm.TemplateBinding;
import org.flasck.flas.parsedForm.TemplateBindingOption;
import org.flasck.flas.parsedForm.TemplateCustomization;
import org.flasck.flas.parsedForm.TemplateField;
import org.flasck.flas.tokenizers.ExprToken;
import org.flasck.flas.tokenizers.Tokenizable;

public class TDATemplateOptionsParser extends BlockLocationTracker implements TDAParsing {
	private final Template source;
	private final TemplateNamer namer;
	private final TemplateBinding binding;
	private final TemplateCustomization customizer;
	private final TemplateField field;
	private boolean seenContent;

	public TDATemplateOptionsParser(ErrorReporter errors, Template source, TemplateNamer namer, TemplateBinding binding, TemplateField field, LocationTracker endofTemplate) {
		super(errors, endofTemplate);
		this.source = source;
		this.namer = namer;
		this.binding = binding;
		this.customizer = binding;
		this.field = field;
	}

	public TDATemplateOptionsParser(ErrorReporter errors, Template source, TemplateNamer namer, TemplateBindingOption option, TemplateField field, LocationTracker endOfTemplate) {
		super(errors, endOfTemplate);
		this.source = source;
		this.namer = namer;
		this.customizer = option;
		this.field = field;
		this.binding = null;
	}

	// This has to handle both options and customization because they are so similar
	// It's important to keep it straight - see the tests for full coverage ...
	@Override
	public TDAParsing tryParsing(Tokenizable toks) {
		seenContent = true;
		ExprToken tok = ExprToken.from(errors, toks);
		if (tok == null) {
			errors.message(toks, "syntax error");
			return new IgnoreNestedParser(errors);
		}
		if ("|".equals(tok.text)) {
			InputPosition barPos = tok.location;
			// it's a conditional - we don't know if it's binding, styling or an error; but "toksHasSend" gives us a clue
			if ((binding == null || binding.defaultBinding != null) && toksHasSend(toks)) {
				errors.message(toks, "conditional bindings are not permitted after the default has been specified");
				return new IgnoreNestedParser(errors);
			}
			else if (binding != null && binding.defaultBinding != null) {
				errors.message(tok.location, "cannot mix bindings and customization");
			}
			if (toksHasSend(toks))
				return TDAParseTemplateElements.parseConditionalBindingOption(errors, barPos, source, namer, toks, field, tbo -> binding.conditionalBindings.add(tbo), parentTracker());
			else
				return TDAParseTemplateElements.parseStyling(errors, tok.location, source, namer, toks, tso -> customizer.conditionalStylings.add(tso), parentTracker());
			
		} else if ("<-".equals(tok.text)) {
			// It's a default send binding
			if (binding == null) {
				errors.message(toks, "syntax error");
				return new IgnoreNestedParser(errors);
			}
			if (binding.defaultBinding != null) {
				errors.message(toks, "multiple default bindings are not permitted");
				return new IgnoreNestedParser(errors);
			}
			return TDAParseTemplateElements.parseDefaultBindingOption(errors, tok.location, source, namer, toks, field, tbo -> binding.defaultBinding = tbo, parentTracker());
		} else if ("=>".equals(tok.text)) {
			// it's an event handler
			return TDAParseTemplateElements.parseEventHandling(tok, errors, source, toks, ev -> customizer.events.add(ev), parentTracker());
		} else {
			errors.message(toks, "syntax error");
			return new IgnoreNestedParser(errors);
		}
	}

	@Override
	public void scopeComplete(InputPosition pos) {
		if (binding != null && !seenContent) {
			errors.message(binding.assignsTo.location(), "simple template name must have options or customization");
			return;
		}
	}

	private boolean toksHasSend(Tokenizable toks) {
		int mark = toks.at();
		boolean ret = false;
		while (toks.hasMoreContent(errors)) {
			ExprToken tok = ExprToken.from(errors, toks);
			if (tok == null)
				break;
			else if (tok.text.equals("<-")) {
				ret = true;
				break;
			}
		}
		toks.reset(mark);
		return ret;
	}
}
