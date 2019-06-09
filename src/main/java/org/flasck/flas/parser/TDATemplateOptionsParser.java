package org.flasck.flas.parser;

import java.util.ArrayList;
import java.util.List;

import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.commonBase.Expr;
import org.flasck.flas.commonBase.StringLiteral;
import org.flasck.flas.errors.ErrorReporter;
import org.flasck.flas.parsedForm.TemplateBinding;
import org.flasck.flas.parsedForm.TemplateBindingOption;
import org.flasck.flas.parsedForm.TemplateEvent;
import org.flasck.flas.parsedForm.TemplateStylingOption;
import org.flasck.flas.tokenizers.ExprToken;
import org.flasck.flas.tokenizers.StringToken;
import org.flasck.flas.tokenizers.TemplateNameToken;
import org.flasck.flas.tokenizers.Tokenizable;

public class TDATemplateOptionsParser implements TDAParsing {
	private final ErrorReporter errors;
	private final TemplateBinding binding;
	private boolean seenContent;

	public TDATemplateOptionsParser(ErrorReporter errors, TemplateBinding binding) {
		this.errors = errors;
		this.binding = binding;
	}

	// This has to handle both options and customization because they are so similar
	// It's important to keep it straight - see the tests for full coverage ...
	@Override
	public TDAParsing tryParsing(Tokenizable toks) {
		seenContent = true;
		ExprToken tok = ExprToken.from(toks);
		if (tok == null) {
			errors.message(toks, "syntax error");
			return new IgnoreNestedParser();
		}
		if ("|".equals(tok.text)) {
			if (binding.defaultBinding != null) {
				errors.message(toks, "conditional bindings are not permitted after the default has been specified");
				return new IgnoreNestedParser();
			}
			List<Expr> seen = new ArrayList<>();
			new TDAExpressionParser(errors, t -> {
				seen.add(t);
			}).tryParsing(toks);
			if (seen.isEmpty()) {
				errors.message(toks, "no conditional expression");
				return new IgnoreNestedParser();
			}
			tok = ExprToken.from(toks);
			if (tok == null) {
				errors.message(toks, "syntax error");
				return new IgnoreNestedParser();
			}
			if ("<-".equals(tok.text)) {
				TemplateBindingOption tbo = readTemplateBinding(toks);
				if (tbo == null)
					return new IgnoreNestedParser();
				binding.conditionalBindings.add(tbo.conditionalOn(seen.get(0)));
			} else if ("=>".equals(tok.text)) {
				TemplateStylingOption tso = readTemplateStyles(seen.get(0), toks);
				if (tso == null)
					return new IgnoreNestedParser();
				binding.conditionalStylings.add(tso);
			} else {
				errors.message(toks, "syntax error");
				return new IgnoreNestedParser();
			}
		} else if ("<-".equals(tok.text)) {
			// It's a default send binding
			if (binding.defaultBinding != null) {
				errors.message(toks, "multiple default bindings are not permitted");
				return new IgnoreNestedParser();
			}
			TemplateBindingOption tbo = readTemplateBinding(toks);
			if (tbo == null)
				return new IgnoreNestedParser();
			binding.defaultBinding = tbo;
		} else if (tok.type == ExprToken.IDENTIFIER) {
			// it's an event handler
			TemplateEvent ev = readEvent(tok, toks);
			if (ev == null)
				return new IgnoreNestedParser();
			binding.events.add(ev);
			return new IgnoreNestedParser();
		} else {
			errors.message(toks, "syntax error");
			return new IgnoreNestedParser();
		}
		return new NoNestingParser(errors);
	}

	private TemplateBindingOption readTemplateBinding(Tokenizable toks) {
		List<Expr> seen = new ArrayList<>();
		new TDAExpressionParser(errors, t -> {
			seen.add(t);
		}).tryParsing(toks);
		if (seen.isEmpty()) {
			errors.message(toks, "no expression to send");
			return null;
		}
		String sendTo = null;
		if (toks.hasMore()) {
			ExprToken sendToTok = ExprToken.from(toks);
			if (sendToTok != null) {
				if ("=>".equals(sendToTok.text)) {
					TemplateNameToken tnt = TemplateNameToken.from(toks);
					sendTo = tnt.text;
				} else {
					errors.message(toks, "syntax error");
					return null;
				}
			} 
		}
		if (toks.hasMore()) {
			errors.message(toks, "syntax error");
			return null;
		}
		return new TemplateBindingOption(null, seen.get(0), sendTo);
	}

	private TemplateStylingOption readTemplateStyles(Expr expr, Tokenizable toks) {
		List<StringLiteral> styles = new ArrayList<>();
		while (toks.hasMore()) {
			InputPosition pos = toks.realinfo();
			String s = StringToken.from(toks);
			if (s == null) {
				errors.message(toks, "invalid style");
				return null;
			}
			styles.add(new StringLiteral(pos, s));
		}
		return new TemplateStylingOption(expr, styles);
	}

	private TemplateEvent readEvent(ExprToken eventName, Tokenizable toks) {
		ExprToken tok = ExprToken.from(toks);
		if (tok == null || !"=>".equals(tok.text)) {
			errors.message(toks, "syntax error");
			return null;
		}
		List<Expr> seen = new ArrayList<>();
		new TDAExpressionParser(errors, t -> {
			seen.add(t);
		}).tryParsing(toks);
		if (seen.isEmpty()) {
			errors.message(toks, "missing event handler");
			return null;
		}
		return new TemplateEvent(eventName.text, seen.get(0));
	}

	@Override
	public void scopeComplete(InputPosition pos) {
		if (!seenContent) {
			errors.message(pos, "simple template name must have options or customization");
			return;
		}
	}

}
