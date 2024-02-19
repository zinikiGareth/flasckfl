package org.flasck.flas.parser;

import java.util.ArrayList;
import java.util.List;

import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.blocker.TDAParsingWithAction;
import org.flasck.flas.commonBase.Expr;
import org.flasck.flas.errors.ErrorReporter;
import org.flasck.flas.parsedForm.Template;
import org.flasck.flas.parsedForm.TemplateBinding;
import org.flasck.flas.parsedForm.TemplateBindingOption;
import org.flasck.flas.parsedForm.TemplateField;
import org.flasck.flas.parsedForm.TemplateReference;
import org.flasck.flas.tokenizers.ExprToken;
import org.flasck.flas.tokenizers.TemplateNameToken;
import org.flasck.flas.tokenizers.Tokenizable;

public class TDATemplateBindingParser extends BlockLocationTracker implements TDAParsing {
	private final Template source;
	private final TemplateNamer namer;
	private final TemplateBindingConsumer consumer;

	public TDATemplateBindingParser(ErrorReporter errors, Template source, TemplateNamer namer, TemplateBindingConsumer consumer, LocationTracker locTracker) {
		super(errors, locTracker);
		this.source = source;
		this.namer = namer;
		this.consumer = consumer;
		updateLoc(source.kwlocation());
	}

	@Override
	public TDAParsing tryParsing(Tokenizable toks) {
		final TemplateNameToken tok = TemplateNameToken.from(errors, toks);
		if (tok == null) {
			ExprToken et = ExprToken.from(errors, toks);
			if (et != null && et.text.equals("|")) {
				return TDAParseTemplateElements.parseStyling(errors, et.location, source, namer, toks, x -> consumer.addStyling(x), this);
			} else {
				errors.message(toks, "syntax error");
				return new IgnoreNestedParser(errors);
			}
		}
		TemplateField field = new TemplateField(tok.location, tok.text);
		InputPosition lastLoc = field.location();
		TemplateBindingOption simple = null;
		ExprToken send = null;
		TemplateReference sendsTo = null;
		if (toks.hasMoreContent(errors)) {
			send = ExprToken.from(errors, toks);
			if (send == null || !"<-".equals(send.text)) {
				if ("=>".equals(send.text))
					errors.message(toks, "missing expression");
				else
					errors.message(toks, "syntax error");
				return new IgnoreNestedParser(errors);
			}
			Expr expr = null;
			if ("<-".equals(send.text)) { 
				List<Expr> seen = new ArrayList<>();
				new TDAExpressionParser(errors, t -> {
					seen.add(t);
				}).tryParsing(toks);
				if (seen.isEmpty()) {
					errors.message(toks, "no expression to send");
					return new IgnoreNestedParser(errors);
				}
				expr = seen.get(0);
				lastLoc = expr.location();
			}
			if (toks.hasMoreContent(errors)) {
				ExprToken format = ExprToken.from(errors, toks);
				if (format == null || !"=>".equals(format.text)) {
					errors.message(toks, "syntax error");
					return new IgnoreNestedParser(errors);
				}
				TemplateNameToken dest = TemplateNameToken.from(errors, toks);
				if (dest == null) {
					errors.message(toks, "missing template name");
					return new IgnoreNestedParser(errors);
				}
				lastLoc = dest.location;
				sendsTo = new TemplateReference(dest.location, namer.template(dest.location, dest.text));
				errors.logReduction("pass-to-template", format, dest);			
			}
			simple = new TemplateBindingOption(tok.location, field, null, expr, sendsTo);
		}
		if (send == null) {
			errors.logReduction("template-binding-no-send", tok.location, lastLoc);
		} else if (sendsTo == null) {
			errors.logReduction("template-binding-with-send", tok.location, lastLoc);			
		} else {
			errors.logReduction("template-binding-with-send-sends-to", tok.location, lastLoc);			
		}
		final TemplateBinding binding = new TemplateBinding(field, simple);
		consumer.addBinding(binding);
		TDAParsing ret;
		if (simple != null)
			ret = new TDATemplateOptionsParser(errors, source, namer, simple, field, this);
		else
			ret = new TDATemplateOptionsParser(errors, source, namer, binding, field, this);
		return new TDAParsingWithAction(ret, reduction(tok.location(), "template-bind"));
	}

	@Override
	public void scopeComplete(InputPosition location) {
	}
}
