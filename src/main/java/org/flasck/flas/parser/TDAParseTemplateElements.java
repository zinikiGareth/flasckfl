package org.flasck.flas.parser;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.commonBase.Expr;
import org.flasck.flas.commonBase.StringLiteral;
import org.flasck.flas.errors.ErrorReporter;
import org.flasck.flas.parsedForm.Template;
import org.flasck.flas.parsedForm.TemplateBindingOption;
import org.flasck.flas.parsedForm.TemplateEvent;
import org.flasck.flas.parsedForm.TemplateField;
import org.flasck.flas.parsedForm.TemplateReference;
import org.flasck.flas.parsedForm.TemplateStylingOption;
import org.flasck.flas.parsedForm.UnresolvedVar;
import org.flasck.flas.tokenizers.ExprToken;
import org.flasck.flas.tokenizers.StringToken;
import org.flasck.flas.tokenizers.TemplateNameToken;
import org.flasck.flas.tokenizers.Tokenizable;
import org.flasck.flas.tokenizers.ValidIdentifierToken;
import org.flasck.flas.tokenizers.VarNameToken;

public class TDAParseTemplateElements {
	public static TDAParsing parseConditionalBindingOption(ErrorReporter errors, Template source, TemplateNamer namer, Tokenizable toks, TemplateField field, Consumer<TemplateBindingOption> consumer) {
		List<Expr> seen = new ArrayList<>();
		new TDAExpressionParser(errors, t -> {
			seen.add(t);
		}).tryParsing(toks);
		ExprToken tok = ExprToken.from(errors, toks);
		if (tok == null) {
			errors.message(toks, "syntax error");
			return new IgnoreNestedParser();
		}
		if ("<-".equals(tok.text)) {
			if (seen.isEmpty()) {
				errors.message(toks, "no conditional expression");
				return new IgnoreNestedParser();
			}
			TemplateBindingOption tbo = readTemplateBinding(errors, namer, toks, field);
			if (tbo == null)
				return new IgnoreNestedParser();
			TemplateBindingOption tc = tbo.conditionalOn(seen.get(0));
			consumer.accept(tc);
			return new TDATemplateOptionsParser(errors, source, namer, tc, field);
		} else {
			errors.message(toks, "syntax error");
			return new IgnoreNestedParser();
		}
	}

	public static TDAParsing parseDefaultBindingOption(ErrorReporter errors, Template source, TemplateNamer namer, Tokenizable toks, TemplateField field, Consumer<TemplateBindingOption> consumer) {
		TemplateBindingOption tc = TDAParseTemplateElements.readTemplateBinding(errors, namer, toks, field);
		if (tc == null)
			return new IgnoreNestedParser();
		consumer.accept(tc);
		return new TDATemplateOptionsParser(errors, source, namer, tc, field);
	}

	public static TDAParsing parseStyling(ErrorReporter errors, Template source, TemplateNamer namer, Tokenizable toks, Consumer<TemplateStylingOption> consumer) {
		List<Expr> seen = new ArrayList<>();
		new TDAExpressionParser(errors, t -> {
			seen.add(t);
		}).tryParsing(toks);
		ExprToken tok = ExprToken.from(errors, toks);
		if (tok == null) {
			errors.message(toks, "syntax error");
			return new IgnoreNestedParser();
		}
		if ("=>".equals(tok.text)) {
			TemplateStylingOption tso = readTemplateStyles(errors, seen.size() == 0 ? null : seen.get(0), toks);
			if (tso == null)
				return new IgnoreNestedParser();
			consumer.accept(tso);
			return new TDATemplateStylingParser(errors, source, namer, tso);
		} else {
			errors.message(toks, "syntax error");
			return new IgnoreNestedParser();
		}

	}

	public static TDAParsing parseEventHandling(ErrorReporter errors, Template source, Tokenizable toks, Consumer<TemplateEvent> consumer) {
		ExprToken tok = ExprToken.from(errors, toks);
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
		TemplateEvent ev = new TemplateEvent(tok.location, tok.text, source);
		consumer.accept(ev);
		return new NoNestingParser(errors);
	}

	public static TemplateBindingOption readTemplateBinding(ErrorReporter errors, TemplateNamer namer, Tokenizable toks, TemplateField field) {
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
					TemplateNameToken tnt = TemplateNameToken.from(errors, toks);
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

	public static TemplateStylingOption readTemplateStyles(ErrorReporter errors, Expr expr, Tokenizable toks) {
		List<Expr> styles = new ArrayList<>();
		while (toks.hasMore()) {
			InputPosition pos = toks.realinfo();
			String s = StringToken.from(errors, toks);
			if (s != null) {
				styles.add(new StringLiteral(pos, s));
				continue;
			}
			ValidIdentifierToken var = VarNameToken.from(toks);
			if (var != null) {
				styles.add(new UnresolvedVar(var.location, var.text));
				continue;
			}
			errors.message(toks, "invalid style");
			return null;
		}
		return new TemplateStylingOption(expr, styles);
	}
}
