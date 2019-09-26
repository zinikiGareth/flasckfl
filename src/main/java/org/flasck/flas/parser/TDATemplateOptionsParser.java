package org.flasck.flas.parser;

import java.util.ArrayList;
import java.util.List;

import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.commonBase.Expr;
import org.flasck.flas.commonBase.StringLiteral;
import org.flasck.flas.errors.ErrorReporter;
import org.flasck.flas.parsedForm.TemplateBinding;
import org.flasck.flas.parsedForm.TemplateBindingOption;
import org.flasck.flas.parsedForm.TemplateCustomization;
import org.flasck.flas.parsedForm.TemplateEvent;
import org.flasck.flas.parsedForm.TemplateStylingOption;
import org.flasck.flas.tokenizers.ExprToken;
import org.flasck.flas.tokenizers.StringToken;
import org.flasck.flas.tokenizers.TemplateNameToken;
import org.flasck.flas.tokenizers.Tokenizable;

public class TDATemplateOptionsParser implements TDAParsing {
	private final ErrorReporter errors;
	private final TemplateBinding binding;
	private final TemplateCustomization customizer;
	private boolean seenContent;

	public TDATemplateOptionsParser(ErrorReporter errors, TemplateBinding binding) {
		this.errors = errors;
		this.binding = binding;
		this.customizer = binding;
	}

	public TDATemplateOptionsParser(ErrorReporter errors, TemplateBindingOption option) {
		this.errors = errors;
		this.binding = null;
		this.customizer = option;
	}

	// This has to handle both options and customization because they are so similar
	// It's important to keep it straight - see the tests for full coverage ...
	@Override
	public TDAParsing tryParsing(Tokenizable toks) {
		seenContent = true;
		ExprToken tok = ExprToken.from(errors, toks);
		if (tok == null) {
			errors.message(toks, "syntax error");
			return new IgnoreNestedParser();
		}
		TemplateBindingOption tc = null;
		if ("|".equals(tok.text)) {
			if ((binding == null || binding.defaultBinding != null) && toksHasSend(toks)) {
				errors.message(toks, "conditional bindings are not permitted after the default has been specified");
				return new IgnoreNestedParser();
			}
			else if (binding != null && binding.defaultBinding != null) {
				errors.message(tok.location, "cannot mix bindings and customization");
			}
			List<Expr> seen = new ArrayList<>();
			new TDAExpressionParser(errors, t -> {
				seen.add(t);
			}).tryParsing(toks);
			if (seen.isEmpty()) {
				errors.message(toks, "no conditional expression");
				return new IgnoreNestedParser();
			}
			tok = ExprToken.from(errors, toks);
			if (tok == null) {
				errors.message(toks, "syntax error");
				return new IgnoreNestedParser();
			}
			if ("<-".equals(tok.text)) {
				TemplateBindingOption tbo = readTemplateBinding(toks);
				if (tbo == null)
					return new IgnoreNestedParser();
				tc = tbo.conditionalOn(seen.get(0));
				binding.conditionalBindings.add(tc);
			} else if ("=>".equals(tok.text)) {
				TemplateStylingOption tso = readTemplateStyles(seen.get(0), toks);
				if (tso == null)
					return new IgnoreNestedParser();
				customizer.conditionalStylings.add(tso);
			} else {
				errors.message(toks, "syntax error");
				return new IgnoreNestedParser();
			}
		} else if ("<-".equals(tok.text)) {
			if (binding == null) {
				errors.message(toks, "syntax error");
				return new IgnoreNestedParser();
			}
			// It's a default send binding
			if (binding.defaultBinding != null) {
				errors.message(toks, "multiple default bindings are not permitted");
				return new IgnoreNestedParser();
			}
			tc = readTemplateBinding(toks);
			if (tc == null)
				return new IgnoreNestedParser();
			binding.defaultBinding = tc;
		} else if (tok.type == ExprToken.IDENTIFIER) {
			// it's an event handler
			TemplateEvent ev = readEvent(tok, toks);
			if (ev == null)
				return new IgnoreNestedParser();
			customizer.events.add(ev);
		} else {
			errors.message(toks, "syntax error");
			return new IgnoreNestedParser();
		}
		if (tc != null)
			return new TDATemplateOptionsParser(errors, tc);
		else
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
			ExprToken sendToTok = ExprToken.from(errors, toks);
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
			String s = StringToken.from(errors, toks);
			if (s == null) {
				errors.message(toks, "invalid style");
				return null;
			}
			styles.add(new StringLiteral(pos, s));
		}
		return new TemplateStylingOption(expr, styles);
	}

	private TemplateEvent readEvent(ExprToken eventName, Tokenizable toks) {
		ExprToken tok = ExprToken.from(errors, toks);
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
		if (binding != null && !seenContent) {
			errors.message(pos, "simple template name must have options or customization");
			return;
		}
	}

	private boolean toksHasSend(Tokenizable toks) {
		int mark = toks.at();
		boolean ret = false;
		while (toks.hasMore()) {
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
