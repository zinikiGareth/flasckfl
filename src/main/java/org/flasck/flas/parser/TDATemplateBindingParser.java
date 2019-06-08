package org.flasck.flas.parser;

import org.flasck.flas.blockForm.InputPosition;
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
		if (toks.hasMore()) {
			ExprToken send = ExprToken.from(toks);
			if (send == null || !"<-".equals(send.text)) {
				errors.message(toks, "syntax error");
				return new IgnoreNestedParser();
			}
		}
		consumer.addBinding(new TemplateBinding(tok.text));
		// TODO: this actually needs to be something that might be this, but might be a customization one depending on what they do
		return new TDATemplateOptionsParser(errors);
	}

	@Override
	public void scopeComplete(InputPosition location) {
		throw new org.zinutils.exceptions.NotImplementedException();
	}

}
