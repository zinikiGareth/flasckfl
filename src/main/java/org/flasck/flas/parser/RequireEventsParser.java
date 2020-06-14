package org.flasck.flas.parser;

import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.errors.ErrorReporter;
import org.flasck.flas.parsedForm.Template;
import org.flasck.flas.parsedForm.TemplateStylingOption;
import org.flasck.flas.tokenizers.ExprToken;
import org.flasck.flas.tokenizers.Tokenizable;

public class RequireEventsParser implements TDAParsing {
	private final ErrorReporter errors;
	private final InputPosition loc;
	private final Template source;
	private final TemplateNamer namer;
	private final TemplateStylingOption tso;
	private boolean seenHandler;

	public RequireEventsParser(ErrorReporter errors, InputPosition location, Template source, TemplateNamer namer, TemplateStylingOption tso) {
		this.errors = errors;
		this.loc = location;
		this.source = source;
		this.namer = namer;
		this.tso = tso;
	}

	@Override
	public TDAParsing tryParsing(Tokenizable toks) {
		ExprToken tok = ExprToken.from(errors, toks);
		if (tok == null) {
			errors.message(toks, "syntax error");
			return new IgnoreNestedParser();
		}
		if ("|".equals(tok.text)) {
			seenHandler = true;
			return TDAParseTemplateElements.parseStyling(errors, source, namer, toks, nested -> tso.conditionalStylings.add(nested));
		} else if ("=>".equals(tok.text)) {
			// it's an event handler
			seenHandler = true;
			return TDAParseTemplateElements.parseEventHandling(errors, source, toks, ev -> tso.events.add(ev));
		} else {
			errors.message(toks, "event handler expected");
			return new IgnoreNestedParser();
		}
	}

	@Override
	public void scopeComplete(InputPosition location) {
		if (!seenHandler) {
			errors.message(loc, "must provide styles and/or nested content");
		}
	}

}
