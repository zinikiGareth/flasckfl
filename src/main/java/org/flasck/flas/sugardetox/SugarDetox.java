package org.flasck.flas.sugardetox;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.flasck.flas.commonBase.ApplyExpr;
import org.flasck.flas.commonBase.Locatable;
import org.flasck.flas.commonBase.NumericLiteral;
import org.flasck.flas.commonBase.StringLiteral;
import org.flasck.flas.commonBase.template.TemplateFormat;
import org.flasck.flas.commonBase.template.TemplateLine;
import org.flasck.flas.errors.ErrorResult;
import org.flasck.flas.parsedForm.CardDefinition;
import org.flasck.flas.parsedForm.ContentExpr;
import org.flasck.flas.parsedForm.ContentString;
import org.flasck.flas.parsedForm.D3Thing;
import org.flasck.flas.parsedForm.EventHandler;
import org.flasck.flas.parsedForm.Scope;
import org.flasck.flas.parsedForm.Template;
import org.flasck.flas.parsedForm.TemplateCardReference;
import org.flasck.flas.parsedForm.TemplateCases;
import org.flasck.flas.parsedForm.TemplateDiv;
import org.flasck.flas.parsedForm.TemplateExplicitAttr;
import org.flasck.flas.parsedForm.TemplateFormatEvents;
import org.flasck.flas.parsedForm.TemplateList;
import org.flasck.flas.parsedForm.TemplateOr;
import org.flasck.flas.parsedForm.TemplateReference;
import org.flasck.flas.parsedForm.UnresolvedOperator;
import org.flasck.flas.parsedForm.UnresolvedVar;
import org.flasck.flas.parsedForm.Scope.ScopeEntry;
import org.flasck.flas.tokenizers.TemplateToken;
import org.zinutils.exceptions.UtilException;

public class SugarDetox {
	private final ErrorResult errors;

	public SugarDetox(ErrorResult errors) {
		this.errors = errors;
	}

	public void detox(Scope scope) {
		for (ScopeEntry x : scope)
			if (x.getValue() instanceof CardDefinition) {
				detoxTemplates(((CardDefinition)x.getValue()));
			}
	}

	private void detoxTemplates(CardDefinition cd) {
		if (cd.templates.isEmpty()) {
			// we have nothing to rewrite, but we need a minimal template - create one
			cd.templates.add(new Template(cd.kw, cd.location, cd.name, null, null));
			return;
		}
		Template t = cd.templates.get(0);
		Template tmp = new Template(t.kw, t.location(), cd.name, null, unroll(errors, cd.templates, cd.d3s, new TreeMap<String, Object>()));
		cd.templates.clear();
		cd.templates.add(tmp);
	}

	private TemplateLine unroll(ErrorResult er, List<Template> templates, List<D3Thing> d3s, Map<String, Object> subst) {
		if (templates == null || templates.isEmpty())
			return null;
		Map<String, Object> map = new TreeMap<String, Object>();
		Template ret = templates.get(0);
		for (Template t : templates) {
			if (t.name == null)
				continue;
			map.put(t.name, t);
		}
		for (D3Thing t : d3s) {
			map.put(t.d3.name, t);
		}
		
		return unroll(er, map, ret.content, subst);
	}

	private TemplateLine unroll(ErrorResult er, Map<String, Object> map, TemplateLine content, Map<String, Object> subst) {
		if (content == null)
			throw new UtilException("Null template line");
		if (content instanceof TemplateCardReference)
			return content;
		if (content instanceof TemplateReference) {
			TemplateReference tr = (TemplateReference) content;
			if (!map.containsKey(tr.name))
				er.message(tr.location, "reference to non-existent template " + tr.name);
			Object reffed = map.get(tr.name);
			if (reffed instanceof Template) {
				Template tt = (Template) reffed;
				if (tr.args.size() != tt.args.size()) {
					er.message(tr.location, "incorrect number of actual parameters to " + tr.name + ": expected " + tt.args.size());
					return null;
				}
				Map<String, Object> nsubst = new TreeMap<String, Object>(subst);
				for (int i=0;i<tr.args.size();i++) {
					String key = tt.args.get(i).text;
					if (nsubst.containsKey(key)) {
						er.message(tr.location, "duplicate binding to formal parameter " + key);
						return null;
					}
					nsubst.put(key, tr.args.get(i));
				}
				return unroll(er, map, tt.content, nsubst);
			} else {
				return (D3Thing) reffed;
			}
		} else if (content instanceof TemplateFormat) {
			TemplateFormat tf = (TemplateFormat) content;
			List<Object> formats = new ArrayList<Object>();
			for (Object o : tf.formats)
				formats.add(substituteMacroParameters(er, map, o, subst));
			if (tf instanceof ContentString) {
				ContentString cs = (ContentString)tf;
				ContentString ret = new ContentString(cs.kw, ((ContentString)tf).text, formats);
				for (EventHandler y : cs.handlers)
					ret.handlers.add(new EventHandler(y.kw, y.actionPos, y.action, substituteMacroParameters(er, map, y.expr, subst)));
				return ret;
			} else if (tf instanceof ContentExpr) {
				ContentExpr ce = (ContentExpr)tf;
				Object sub = substituteMacroParameters(er, map, ce.expr, subst);
				TemplateFormatEvents ret;
				if (sub instanceof StringLiteral) {
					StringLiteral sl = (StringLiteral)sub;
					ret = new ContentString(sl.location, sl.text, formats);
				} else if (sub instanceof NumericLiteral) {
					NumericLiteral nl = (NumericLiteral)sub;
					ret = new ContentString(nl.location, nl.text, formats);
				} else if (sub instanceof Locatable) {
					Locatable l = (Locatable) sub;
					ret = new ContentExpr(l.location(), sub, ce.editable(), ce.rawHTML, formats);
				} else
					throw new UtilException("Cannot substitute " + sub);
				for (EventHandler y : ce.handlers)
					ret.handlers.add(new EventHandler(y.kw, y.actionPos, y.action, substituteMacroParameters(er, map, y.expr, subst)));
				return ret;
			} else if (tf instanceof TemplateDiv) {
				TemplateDiv td = (TemplateDiv) tf;
				List<Object> attrs = new ArrayList<Object>();
				for (Object o : td.attrs)
					attrs.add(substituteMacroParameters(er, map, o, subst));
				TemplateDiv ret = new TemplateDiv(td.kw, td.customTagLoc, td.customTag, td.customTagVarLoc, td.customTagVar, attrs, formats);
				for (TemplateLine x : td.nested)
					ret.nested.add(unroll(er, map, x, subst));
				for (EventHandler y : td.handlers)
					ret.handlers.add(new EventHandler(y.kw, y.actionPos, y.action, substituteMacroParameters(er, map, y.expr, subst)));
				return ret;
			} else if (tf instanceof TemplateList) {
				TemplateList tl = (TemplateList) tf;
				TemplateList ret = new TemplateList(tl.kw, tl.listLoc, tl.listExpr, tl.iterLoc, tl.iterVar, tl.customTagLoc, tl.customTag, tl.customTagVarLoc, tl.customTagVar, formats, false);
				ret.template = unroll(er, map, tl.template, subst);
				return ret;
			}
			else
				throw new UtilException("Not supported: " + tf.getClass());
		} else if (content instanceof TemplateCases) {
			TemplateCases tc = (TemplateCases) content;
			TemplateCases ret = new TemplateCases(tc.loc, substituteMacroParameters(er, map, tc.switchOn, subst));
			for (TemplateOr i : tc.cases)
				ret.cases.add((TemplateOr) unroll(er, map, i, subst));
			return ret;
		} else if (content instanceof TemplateOr) {
			TemplateOr tc = (TemplateOr) content;
			return new TemplateOr(tc.location(), substituteMacroParameters(er, map, tc.cond, subst),  unroll(er, map, tc.template, subst));
		} else
			throw new UtilException("Not handled: " + content.getClass());
	}

	private Object substituteMacroParameters(ErrorResult er, Map<String, Object> map, Object o, Map<String, Object> subst) {
		if (o == null)
			return null;
		else if (o instanceof StringLiteral || o instanceof NumericLiteral)
			return o;
		else if (o instanceof TemplateToken) {
			TemplateToken tt = (TemplateToken) o;
			if (tt.type == TemplateToken.IDENTIFIER && subst.containsKey(tt.text))
				return asTT(subst.get(tt.text));
		} else if (o instanceof TemplateExplicitAttr) {
			TemplateExplicitAttr tea = (TemplateExplicitAttr) o;
			if (tea.type == TemplateToken.IDENTIFIER) { // any kind of expression
				return new TemplateExplicitAttr(tea.location, tea.attr, tea.type, substituteMacroParameters(er, map, tea.value, subst));
			} else if (tea.type == TemplateToken.STRING) {
				return tea;
			} else
				throw new UtilException("Cannot handle: " + tea);
		} else if (o instanceof UnresolvedVar) {
			String str = ((UnresolvedVar)o).var;
			if (subst.containsKey(str))
				return subst.get(str);
			return o;
		} else if (o instanceof UnresolvedOperator) {
			return o;
		} else if (o instanceof ApplyExpr) {
			ApplyExpr ae = (ApplyExpr) o;
			List<Object> args = new ArrayList<Object>();
			for (Object o2 : ae.args)
				args.add(substituteMacroParameters(er, map, o2, subst));
			return new ApplyExpr(ae.location, substituteMacroParameters(er, map, ae.fn, subst), args);
		} else if (o instanceof TemplateCardReference) {
			// We don't have any parameters in this yet that could be macro parameters
		} else if (o instanceof TemplateCases) {
			TemplateCases tc = (TemplateCases)o;
			TemplateCases ret = new TemplateCases(tc.loc, substituteMacroParameters(er, map, tc.switchOn, subst));
			for (TemplateOr x : tc.cases)
				ret.addCase((TemplateOr) substituteMacroParameters(er, map, x, subst));
			return ret;
		} else if (o instanceof TemplateOr) {
			TemplateOr tor = (TemplateOr) o;
			TemplateOr ret = new TemplateOr(tor.location(), substituteMacroParameters(er, map, tor.cond, subst), unroll(er, map, tor.template, subst));
			return ret;
		} else
			System.out.println("subMacroParms cannot handle: " + o + " "  + o.getClass());
			
		return o;
	}

	private TemplateToken asTT(Object sub) {
		if (sub instanceof StringLiteral) {
			StringLiteral s = (StringLiteral) sub;
			return new TemplateToken(s.location, TemplateToken.STRING, s.text, -1);
		}
		throw new UtilException("Cannot handle: " + sub + ": " + sub.getClass());
	}
}
