package org.flasck.flas.parser;

import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.errors.ErrorReporter;
import org.flasck.flas.parsedForm.Template;
import org.flasck.flas.parsedForm.TemplateStylingOption;
import org.flasck.flas.tokenizers.ExprToken;
import org.flasck.flas.tokenizers.Tokenizable;

public class RequireEventsParser extends BlockLocationTracker implements TDAParsing {
	private final InputPosition loc;
	private final Template source;
	private final TemplateNamer namer;
	private final TemplateStylingOption tso;
	private boolean seenHandler;

	public RequireEventsParser(ErrorReporter errors, InputPosition location, Template source, TemplateNamer namer, TemplateStylingOption tso, LocationTracker locTracker) {
		super(errors, locTracker);
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
			return new IgnoreNestedParser(errors);
		}
		if ("|".equals(tok.text)) {
			seenHandler = true;
			return TDAParseTemplateElements.parseStyling(errors, tok.location, source, namer, toks, nested -> tso.conditionalStylings.add(nested), this);
		} else if ("=>".equals(tok.text)) {
			// it's an event handler
			seenHandler = true;
			return TDAParseTemplateElements.parseEventHandling(tok, errors, source, toks, ev -> tso.events.add(ev), this);
		} else {
			errors.message(toks, "event handler expected");
			return new IgnoreNestedParser(errors);
		}
	}

	@Override
	public void updateLoc(InputPosition location) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void scopeComplete(InputPosition location) {
		if (!seenHandler) {
			errors.message(loc, "must provide styles and/or nested content");
		}
	}

}
