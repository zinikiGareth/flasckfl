package org.flasck.flas.template;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.commonBase.ApplyExpr;
import org.flasck.flas.commonBase.Locatable;
import org.flasck.flas.commonBase.StringLiteral;
import org.flasck.flas.commonBase.template.TemplateCases;
import org.flasck.flas.commonBase.template.TemplateExplicitAttr;
import org.flasck.flas.commonBase.template.TemplateFormat;
import org.flasck.flas.commonBase.template.TemplateLine;
import org.flasck.flas.commonBase.template.TemplateList;
import org.flasck.flas.commonBase.template.TemplateOr;
import org.flasck.flas.errors.ErrorResult;
import org.flasck.flas.parsedForm.TemplateFormatEvents;
import org.flasck.flas.rewriter.Rewriter;
import org.flasck.flas.rewrittenForm.CardMember;
import org.flasck.flas.rewrittenForm.RWContentExpr;
import org.flasck.flas.rewrittenForm.RWContentString;
import org.flasck.flas.rewrittenForm.RWEventHandler;
import org.flasck.flas.rewrittenForm.RWFunctionCaseDefn;
import org.flasck.flas.rewrittenForm.RWFunctionDefinition;
import org.flasck.flas.rewrittenForm.RWFunctionIntro;
import org.flasck.flas.rewrittenForm.RWTemplate;
import org.flasck.flas.rewrittenForm.RWTemplateCardReference;
import org.flasck.flas.rewrittenForm.RWTemplateDiv;
import org.flasck.flas.rewrittenForm.RWTemplateFormatEvents;
import org.flasck.flas.tokenizers.TemplateToken;
import org.flasck.flas.vcode.hsieForm.HSIEForm.CodeType;
import org.zinutils.exceptions.UtilException;

public class TemplateFunctionGenerator {
	private class State {
		private final String prefix;
		private int idx = 0;

		public State(String prefix) {
			this.prefix = prefix;
		}

		public String nextFunction(String type) {
			return this.prefix+"."+type+"_"+(idx++);
		}
	}

	private final Rewriter rw;
	private final Map<String, RWFunctionDefinition> functions;
	private final Object nil;
	private final Object cons;
	private final Object equals;
	private final Map<TemplateLine, String> formats = new HashMap<>();
	private final Map<TemplateLine, String> handlers = new HashMap<>();
	private final Map<TemplateExplicitAttr, String> teas = new HashMap<>();

	public TemplateFunctionGenerator(ErrorResult errors, Rewriter rewriter, Map<String, RWFunctionDefinition> functions) {
		this.rw = rewriter;
		this.functions = functions;
		InputPosition posn = new InputPosition("tfg", 1, 1, "");
		this.nil = rw.getMe(posn, "Nil");
		this.cons = rw.getMe(posn, "Cons");
		this.equals = rw.getMe(posn, "==");
	}

	public void generate() {
		for (RWTemplate x : rw.templates) {
			System.out.println("Generating template functions for " + x);
			recurse(new State(x.prefix), x.content);
		}
	}

	private void recurse(State state, TemplateLine content) {
		if (content == null)
			return;

		boolean isEditable = false;
		if (content instanceof TemplateFormatEvents) {
			TemplateFormatEvents tf = (TemplateFormatEvents) content;
			for (Object q : tf.handlers) {
				throw new UtilException("Cannot handle " + q.getClass());
			}
		}
		if (content instanceof RWTemplateDiv) {
			RWTemplateDiv td = (RWTemplateDiv) content;
			for (Object a : td.attrs) {
				if (a instanceof TemplateExplicitAttr) {
					TemplateExplicitAttr tea = (TemplateExplicitAttr) a;
					if (tea.type == TemplateToken.IDENTIFIER) {
						String fnName = state.nextFunction("teas");
						RWFunctionDefinition fn = new RWFunctionDefinition(tea.location, CodeType.AREA, fnName, 0, true);
						RWFunctionCaseDefn fcd0 = new RWFunctionCaseDefn(new RWFunctionIntro(tea.location, fnName, new ArrayList<>(), null), 0, tea.value);
						fn.cases.add(fcd0);
						functions.put(fnName, fn);
						teas.put(tea, fnName);
					}
				}
			}
			for (TemplateLine x : td.nested)
				recurse(state, x);
		} else if (content instanceof TemplateList) {
			TemplateList l = (TemplateList) content;
			recurse(state, l.template);
		} else if (content instanceof TemplateCases) {
			TemplateCases cs = (TemplateCases) content;
			// more to do (switchOn) 
			for (TemplateOr x : cs.cases)
				recurse(state, x);
		} else if (content instanceof TemplateOr) {
			TemplateOr cs = (TemplateOr) content;
			// more to do (cond)
			recurse(state, cs.template);
		} else if (content instanceof RWTemplateCardReference) {
			RWTemplateCardReference ref = (RWTemplateCardReference) content;
			// more to do (yoyoVar)
		} else if (content instanceof RWContentExpr) {
			isEditable = ((RWContentExpr)content).editable();
		} else if (content instanceof RWContentString) {
			; // I don't think anything needs to be done specifically for this (it could have formats or handlers)
		} else
			throw new UtilException("Cannot handle " + content.getClass());
		if (content instanceof TemplateFormat) {
			StringBuilder simple = new StringBuilder();
			if (isEditable)
				simple.append(" flasck-editable");
			TemplateFormat tf = (TemplateFormat) content;
			Object expr = null;
			InputPosition first = null;
			for (Object o : tf.formats) {
				if (o instanceof TemplateToken) {
					TemplateToken tt = (TemplateToken) o;
					if (tt.type == TemplateToken.STRING) {
						simple.append(" ");
						simple.append(tt.text);
						first = tt.location;
					} else {
						System.out.println(tt);
						throw new UtilException("Cannot handle format of type " + tt.type);
					}
				} else if (o instanceof ApplyExpr || o instanceof CardMember) {
					// TODO: need to collect object/field pairs that we depend on
					if (expr == null)
						expr = nil;
					expr = new ApplyExpr(((Locatable)o).location(), cons, o, expr);
				} else
					throw new UtilException("Cannot handle format of type " + o.getClass());
			}
			if (expr != null) {
				if (simple.length() > 0)
					expr = new ApplyExpr(first, cons, new StringLiteral(first, simple.substring(1)), expr);
				String fnName = state.nextFunction("formats");
				RWFunctionDefinition fn = new RWFunctionDefinition(tf.kw, CodeType.AREA, fnName, 0, true);
				RWFunctionCaseDefn fcd0 = new RWFunctionCaseDefn(new RWFunctionIntro(tf.kw, fnName, new ArrayList<>(), null), 0, expr);
				fn.cases.add(fcd0);
				functions.put(fnName, fn);
				formats.put(tf, fnName);
			}
		}
		if (content instanceof RWTemplateFormatEvents) {
			RWTemplateFormatEvents tfe = (RWTemplateFormatEvents) content;
			for (RWEventHandler eh : tfe.handlers) {
				String fnName = state.nextFunction("handlers");
				InputPosition loc = ((Locatable)eh.expr).location();
				RWFunctionDefinition fn = new RWFunctionDefinition(loc, CodeType.AREA, fnName, 0, true);
				RWFunctionCaseDefn fcd0 = new RWFunctionCaseDefn(new RWFunctionIntro(loc, fnName, new ArrayList<>(), null), 0, eh.expr);
				fn.cases.add(fcd0);
				functions.put(fnName, fn);
				handlers.put(tfe, fnName);
			}
		}
	}
}
