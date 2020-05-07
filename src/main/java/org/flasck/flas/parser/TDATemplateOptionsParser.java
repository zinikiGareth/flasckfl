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
import org.flasck.flas.parsedForm.TemplateField;
import org.flasck.flas.parsedForm.TemplateReference;
import org.flasck.flas.parsedForm.TemplateStylingOption;
import org.flasck.flas.tokenizers.ExprToken;
import org.flasck.flas.tokenizers.StringToken;
import org.flasck.flas.tokenizers.TemplateNameToken;
import org.flasck.flas.tokenizers.Tokenizable;

public class TDATemplateOptionsParser implements TDAParsing {
	private final ErrorReporter errors;
	private final TemplateNamer namer;
	private final TemplateBinding binding;
	private final TemplateCustomization customizer;
	private final TemplateField field;
	private boolean seenContent;

	public TDATemplateOptionsParser(ErrorReporter errors, TemplateNamer namer, TemplateBinding binding, TemplateField field) {
		this.errors = errors;
		this.namer = namer;
		this.binding = binding;
		this.customizer = binding;
		this.field = field;
	}

	public TDATemplateOptionsParser(ErrorReporter errors, TemplateNamer namer, TemplateBindingOption option, TemplateField field) {
		this.errors = errors;
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
			tok = ExprToken.from(errors, toks);
			if (tok == null) {
				errors.message(toks, "syntax error");
				return new IgnoreNestedParser();
			}
			if ("<-".equals(tok.text)) {
				if (seen.isEmpty()) {
					errors.message(toks, "no conditional expression");
					return new IgnoreNestedParser();
				}
				TemplateBindingOption tbo = readTemplateBinding(toks, field);
				if (tbo == null)
					return new IgnoreNestedParser();
				tc = tbo.conditionalOn(seen.get(0));
				binding.conditionalBindings.add(tc);
			} else if ("=>".equals(tok.text)) {
				TemplateStylingOption tso = readTemplateStyles(field, seen.size() == 0 ? null : seen.get(0), toks);
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
			tc = readTemplateBinding(toks, field);
			if (tc == null)
				return new IgnoreNestedParser();
			binding.defaultBinding = tc;
		} else if ("=>".equals(tok.text)) {
			// it's an event handler
			tok = ExprToken.from(errors, toks);
			if (tok == null) {
				errors.message(toks, "event handler name required");
				return new IgnoreNestedParser();
			}
			else if (tok.type != ExprToken.IDENTIFIER) {
				errors.message(tok.location, "event handler name required");
				return new IgnoreNestedParser();
			}
			if (toks.hasMore()) {
				errors.message(toks, "surplus text at end of line");
				return new IgnoreNestedParser();
			}
			TemplateEvent ev = new TemplateEvent(tok.location, tok.text);
			customizer.events.add(ev);
		} else {
			errors.message(toks, "syntax error");
			return new IgnoreNestedParser();
		}
		if (tc != null)
			return new TDATemplateOptionsParser(errors, namer, tc, field);
		else
			return new NoNestingParser(errors);
	}

	private TemplateBindingOption readTemplateBinding(Tokenizable toks, TemplateField field) {
		List<Expr> seen = new ArrayList<>();
		new TDAExpressionParser(errors, t -> {
			seen.add(t);
		}).tryParsing(toks);
		if (seen.isEmpty()) {
			errors.message(toks, "no expression to send");
			return null;
		}
		TemplateReference sendTo = null;
		if (toks.hasMore()) {
			ExprToken sendToTok = ExprToken.from(errors, toks);
			if (sendToTok != null) {
				if ("=>".equals(sendToTok.text)) {
					TemplateNameToken tnt = TemplateNameToken.from(toks);
					sendTo = new TemplateReference(tnt.location, namer.template(tnt.location, tnt.text));
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
		return new TemplateBindingOption(field, null, seen.get(0), sendTo);
	}

	private TemplateStylingOption readTemplateStyles(TemplateField field, Expr expr, Tokenizable toks) {
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
		return new TemplateStylingOption(field, expr, styles);
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
