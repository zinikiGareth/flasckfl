package org.flasck.flas.parser;

import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.errors.ErrorReporter;
import org.flasck.flas.parsedForm.Template;
import org.flasck.flas.parsedForm.TemplateCustomization;
import org.flasck.flas.parsedForm.TemplateStylingOption;
import org.flasck.flas.tokenizers.ExprToken;
import org.flasck.flas.tokenizers.Tokenizable;

public class TDATemplateStylingParser extends BlockLocationTracker implements TDAParsing {
	private final Template source;
	private final TemplateNamer namer;
	private final TemplateCustomization customizer;

	public TDATemplateStylingParser(ErrorReporter errors, Template source, TemplateNamer namer, TemplateStylingOption tso, LocationTracker locTracker) {
		super(errors, locTracker);
		this.source = source;
		this.namer = namer;
		this.customizer = tso;
	}

	@Override
	public TDAParsing tryParsing(Tokenizable toks) {
		ExprToken tok = ExprToken.from(errors, toks);
		if (tok == null) {
			errors.message(toks, "syntax error");
			return new IgnoreNestedParser(errors);
		}
		tellParent(tok.location);
		if ("|".equals(tok.text)) {
			return TDAParseTemplateElements.parseStyling(errors, tok.location, source, namer, toks, tso -> customizer.conditionalStylings.add(tso), this);
		} else if ("=>".equals(tok.text)) {
			// it's an event handler
			return TDAParseTemplateElements.parseEventHandling(tok, errors, source, toks, ev -> customizer.events.add(ev), this);
		} else {
			errors.message(toks, "syntax error");
			return new IgnoreNestedParser(errors);
		}
	}

	@Override
	public void scopeComplete(InputPosition pos) {
	}
}
