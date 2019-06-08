package org.flasck.flas.parser;

import java.util.ArrayList;
import java.util.List;

import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.commonBase.Expr;
import org.flasck.flas.errors.ErrorReporter;
import org.flasck.flas.parsedForm.TemplateBinding;
import org.flasck.flas.tokenizers.ExprToken;
import org.flasck.flas.tokenizers.TemplateNameToken;
import org.flasck.flas.tokenizers.Tokenizable;

public class TDATemplateBindingParser implements TDAParsing {
	private final ErrorReporter errors;
	private final TemplateBindingConsumer consumer;

	public TDATemplateBindingParser(ErrorReporter errors, TemplateBindingConsumer consumer) {
		this.errors = errors;
		this.consumer = consumer;
	}

	@Override
	public TDAParsing tryParsing(Tokenizable toks) {
		TemplateNameToken tok = TemplateNameToken.from(toks);
		if (tok == null) {
			errors.message(toks, "syntax error");
			return new IgnoreNestedParser();
		}
		Expr expr = null;
		String sendsTo = null;
		if (toks.hasMore()) {
			ExprToken send = ExprToken.from(toks);
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
			expr = seen.get(0);
			if (toks.hasMore()) {
				ExprToken format = ExprToken.from(toks);
				if (format == null || !"=>".equals(format.text)) {
					errors.message(toks, "syntax error");
					return new IgnoreNestedParser();
				}
				TemplateNameToken dest = TemplateNameToken.from(toks);
				if (dest == null) {
					errors.message(toks, "missing template name");
					return new IgnoreNestedParser();
				}
				sendsTo = dest.text;
			}
		}
		consumer.addBinding(new TemplateBinding(tok.text, expr, sendsTo));
		// TODO: this actually needs to be something that might be this, but might be a customization one depending on what they do
		return new TDATemplateOptionsParser(errors);
	}

	@Override
	public void scopeComplete(InputPosition location) {
		throw new org.zinutils.exceptions.NotImplementedException();
	}

}
