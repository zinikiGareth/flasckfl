package org.flasck.flas.parser;

import java.util.ArrayList;
import java.util.List;

import org.flasck.flas.blockForm.InputPosition;
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

public class TDATemplateBindingParser implements TDAParsing {
	private final ErrorReporter errors;
	private final Template source;
	private final TemplateNamer namer;
	private final TemplateBindingConsumer consumer;

	public TDATemplateBindingParser(ErrorReporter errors, Template source, TemplateNamer namer, TemplateBindingConsumer consumer) {
		this.errors = errors;
		this.source = source;
		this.namer = namer;
		this.consumer = consumer;
	}

	@Override
	public TDAParsing tryParsing(Tokenizable toks) {
		final TemplateNameToken tok = TemplateNameToken.from(errors, toks);
		if (tok == null) {
			ExprToken et = ExprToken.from(errors, toks);
			if (et != null && et.text.equals("|")) {
				return TDAParseTemplateElements.parseStyling(errors, source, namer, toks, x -> consumer.addStyling(x));
			} else {
				errors.message(toks, "syntax error");
				return new IgnoreNestedParser();
			}
		}
		TemplateField field = new TemplateField(tok.location, tok.text);
		TemplateBindingOption simple = null;
		if (toks.hasMoreContent()) {
			ExprToken send = ExprToken.from(errors, toks);
			if (send == null || !"<-".equals(send.text)) {
				if ("=>".equals(send.text))
					errors.message(toks, "missing expression");
				else
					errors.message(toks, "syntax error");
				return new IgnoreNestedParser();
			}
			List<Expr> seen = new ArrayList<>();
			new TDAExpressionParser(errors, t -> {
				seen.add(t);
			}).tryParsing(toks);
			if (seen.isEmpty()) {
				errors.message(toks, "no expression to send");
				return new IgnoreNestedParser();
			}
			Expr expr = seen.get(0);
			TemplateReference sendsTo = null;
			if (toks.hasMoreContent()) {
				ExprToken format = ExprToken.from(errors, toks);
				if (format == null || !"=>".equals(format.text)) {
					errors.message(toks, "syntax error");
					return new IgnoreNestedParser();
				}
				TemplateNameToken dest = TemplateNameToken.from(errors, toks);
				if (dest == null) {
					errors.message(toks, "missing template name");
					return new IgnoreNestedParser();
				}
				sendsTo = new TemplateReference(dest.location, namer.template(dest.location, dest.text));
			}
			simple = new TemplateBindingOption(field, null, expr, sendsTo);
		}
		final TemplateBinding binding = new TemplateBinding(field, simple);
		consumer.addBinding(binding);
		if (simple != null)
			return new TDATemplateOptionsParser(errors, source, namer, simple, field);
		else
			return new TDATemplateOptionsParser(errors, source, namer, binding, field);
	}

	@Override
	public void scopeComplete(InputPosition location) {
	}

}
