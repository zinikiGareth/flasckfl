package org.flasck.flas.parser;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.blocker.TDAParsingWithAction;
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
import org.flasck.flas.tokenizers.TemplateNameToken;
import org.flasck.flas.tokenizers.Tokenizable;

public class TDAParseTemplateElements {
	public static TDAParsing parseConditionalBindingOption(ErrorReporter errors, InputPosition barPos, Template source, TemplateNamer namer, Tokenizable toks, TemplateField field, Consumer<TemplateBindingOption> consumer, LocationTracker tracker) {
		List<Expr> seen = new ArrayList<>();
		new TDAExpressionParser(errors, t -> {
			seen.add(t);
		}).tryParsing(toks);
		ExprToken tok = ExprToken.from(errors, toks);
		if (tok == null) {
			errors.message(toks, "syntax error");
			return new IgnoreNestedParser(errors);
		}
		if ("<-".equals(tok.text)) {
			if (seen.isEmpty()) {
				errors.message(toks, "no conditional expression");
				return new IgnoreNestedParser(errors);
			}
			TemplateBindingOption tbo = readTemplateBinding(errors, namer, toks, field);
			if (tbo == null)
				return new IgnoreNestedParser(errors);
			TemplateBindingOption tc = tbo.conditionalOn(barPos, seen.get(0));
			consumer.accept(tc);
			errors.logReduction("template-conditional-binding", barPos, tbo.location());
			if (tracker != null)
				tracker.updateLoc(barPos);
			return new TDATemplateOptionsParser(errors, source, namer, tc, field, tracker);
		} else {
			errors.message(toks, "syntax error");
			return new IgnoreNestedParser(errors);
		}
	}

	public static TDAParsing parseDefaultBindingOption(ErrorReporter errors, InputPosition sendPos, Template source, TemplateNamer namer, Tokenizable toks, TemplateField field, Consumer<TemplateBindingOption> consumer, LocationTracker tracker) {
		TemplateBindingOption tc = TDAParseTemplateElements.readTemplateBinding(errors, namer, toks, field);
		if (tc == null)
			return new IgnoreNestedParser(errors);
		consumer.accept(tc);
		errors.logReduction("template-default-binding", sendPos, tc.location());
		if (tracker != null)
			tracker.updateLoc(sendPos);
		return new TDATemplateOptionsParser(errors, source, namer, tc, field, tracker);
	}

	public static TDAParsing parseStyling(ErrorReporter errors, InputPosition barPos, Template source, TemplateNamer namer, Tokenizable toks, Consumer<TemplateStylingOption> consumer, LocationTracker locTracker) {
		locTracker.updateLoc(barPos);
		List<Expr> seen = new ArrayList<>();
		new TDAExpressionParser(errors, t -> {
			seen.add(t);
		}).tryParsing(toks);
		Expr expr = seen.size() == 0 ? null : seen.get(0);
		ExprToken tok = ExprToken.from(errors, toks);
		BlockLocationTracker blt = new BlockLocationTracker(errors, locTracker);
		if (tok == null) {
			TemplateStylingOption tso = new TemplateStylingOption(barPos, expr, new ArrayList<>(), null);
			consumer.accept(tso);
			errors.logReduction("template-conditional-styling-no-actions", barPos, expr.location());
			return new TDAParsingWithAction(
				new RequireEventsParser(errors, toks.realinfo(), source, namer, tso, blt),
				blt.reduction(barPos, "template-style-cond")
			);
		}
		if ("=>".equals(tok.text)) {
			TemplateStylingOption tso = readTemplateStyles(barPos, errors, expr, toks, blt);
			if (tso == null)
				return new IgnoreNestedParser(errors);
			consumer.accept(tso);
			if (tso.orelse != null) {
				errors.logReduction("template-style-format-or-else", barPos, tso.orelse.get(tso.orelse.size()-1).location());
				return new NoNestingParser(errors, "cannot nest more options inside a cond-or-else style");
			}
			return new TDAParsingWithAction(
				new TDATemplateStylingParser(errors, source, namer, tso, blt),
				blt.reduction(barPos, "template-style-format")
			);
		} else {
			errors.message(toks, "=> required for styling");
			return new IgnoreNestedParser(errors);
		}
	}

	public static TDAParsing parseEventHandling(ExprToken arrow, ErrorReporter errors, Template source, Tokenizable toks, Consumer<TemplateEvent> consumer, LocationTracker locTracker) {
		ExprToken tok = ExprToken.from(errors, toks);
		if (tok == null) {
			errors.message(toks, "event handler name required");
			return new IgnoreNestedParser(errors);
		}
		else if (tok.type != ExprToken.IDENTIFIER) {
			errors.message(tok.location, "event handler name required");
			return new IgnoreNestedParser(errors);
		}
		if (toks.hasMoreContent(errors)) {
			errors.message(toks, "surplus text at end of line");
			return new IgnoreNestedParser(errors);
		}
		TemplateEvent ev = new TemplateEvent(tok.location, tok.text, source);
		consumer.accept(ev);
		errors.logReduction("template-event", arrow.location, tok.location);
		locTracker.updateLoc(arrow.location);
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
		if (toks.hasMoreContent(errors)) {
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
		if (toks.hasMoreContent(errors)) {
			errors.message(toks, "syntax error");
			return null;
		}
		return new TemplateBindingOption(seen.get(0).location(), field, null, seen.get(0), sendTo);
	}

	public static TemplateStylingOption readTemplateStyles(InputPosition barPos, ErrorReporter errors, Expr expr, Tokenizable toks, LocationTracker locTracker) {
		List<Expr> styles = new ArrayList<>();
		List<Expr> orelse = null;
		List<Expr> addTo = styles;
		InputPosition lastLoc = barPos;
		if (expr != null)
			lastLoc = expr.location();
		while (toks.hasMoreContent(errors)) {
			int mark = toks.at();
			ExprToken et = ExprToken.from(errors, toks);
			if (et != null) {
				lastLoc = et.location;
				if (et.type == ExprToken.IDENTIFIER) {
					addTo.add(new UnresolvedVar(et.location, et.text));
					continue;
				} else if (et.type == ExprToken.STRING) {
					addTo.add(new StringLiteral(et.location, et.text));
					continue;
				} else if (et.type == ExprToken.SYMBOL) {
					if (et.text.equals("||")) {
						if (expr == null) {
							errors.message(et.location, "cannot use || without a condition");
							return null;
						}
						if (orelse != null) {
							errors.message(et.location, "cannot use || more than once");
							return null;
						}
						orelse = new ArrayList<>();
						addTo = orelse;
						continue;
					}
				} else if (et.type == ExprToken.PUNC) {
					if (et.text.equals("(")) {
						toks.reset(mark);
						List<Expr> ret = new ArrayList<>();
						Consumer<Expr> handler = new Consumer<Expr>() {
							@Override
							public void accept(Expr t) {
								ret.add(t);
							}
						};
						// then it's an expression, we can allow that ...
						TDAExpressionParser ep = new TDAExpressionParser(errors, handler);
						ep.tryParsing(toks);
						if (ret.size() != 1) {
							errors.message(et.location, "valid style expected");
							return null;
						}
//						ExprToken crb = ExprToken.from(errors, toks);
//						if (crb == null) {
//							errors.message(toks, "expected )");
//							return null;
//						} else if (!crb.text.equals(")")) {
//							errors.message(crb.location, "expected )");
//							return null;
//						}
//						errors.logReduction("orb-closed-top", et.location, crb.location);
						addTo.add(ret.get(0));
						continue;
					}
				}
				errors.message(et.location, "valid style expected");
				return null;
			}
			errors.message(toks, "valid style expected");
			return null;
		}
		errors.logReduction("template-styling-option", barPos, lastLoc);
		if (locTracker != null)
			locTracker.updateLoc(barPos);
		return new TemplateStylingOption(barPos, expr, styles, orelse);
	}
}
