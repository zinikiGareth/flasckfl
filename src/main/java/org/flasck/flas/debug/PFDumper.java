package org.flasck.flas.debug;

import java.util.List;

import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.blockForm.LocatedToken;
import org.flasck.flas.commonBase.ApplyExpr;
import org.flasck.flas.commonBase.ConstPattern;
import org.flasck.flas.commonBase.IfExpr;
import org.flasck.flas.commonBase.Locatable;
import org.flasck.flas.commonBase.NumericLiteral;
import org.flasck.flas.commonBase.StringLiteral;
import org.flasck.flas.parsedForm.CardDefinition;
import org.flasck.flas.parsedForm.CastExpr;
import org.flasck.flas.parsedForm.ConstructorMatch;
import org.flasck.flas.parsedForm.ContentExpr;
import org.flasck.flas.parsedForm.ContentString;
import org.flasck.flas.parsedForm.ContractDecl;
import org.flasck.flas.parsedForm.ContractImplements;
import org.flasck.flas.parsedForm.ContractMethodDecl;
import org.flasck.flas.parsedForm.ContractService;
import org.flasck.flas.parsedForm.D3Intro;
import org.flasck.flas.parsedForm.D3PatternBlock;
import org.flasck.flas.parsedForm.D3Section;
import org.flasck.flas.parsedForm.D3Thing;
import org.flasck.flas.parsedForm.EventCaseDefn;
import org.flasck.flas.parsedForm.EventHandler;
import org.flasck.flas.parsedForm.FunctionCaseDefn;
import org.flasck.flas.parsedForm.FunctionTypeReference;
import org.flasck.flas.parsedForm.HandlerImplements;
import org.flasck.flas.parsedForm.IScope;
import org.flasck.flas.parsedForm.LocatedName;
import org.flasck.flas.parsedForm.MethodCaseDefn;
import org.flasck.flas.parsedForm.MethodMessage;
import org.flasck.flas.parsedForm.ObjectDefn;
import org.flasck.flas.parsedForm.PolyType;
import org.flasck.flas.parsedForm.PropertyDefn;
import org.flasck.flas.parsedForm.Scope.ScopeEntry;
import org.flasck.flas.parsedForm.StateDefinition;
import org.flasck.flas.parsedForm.StructDefn;
import org.flasck.flas.parsedForm.StructField;
import org.flasck.flas.parsedForm.Template;
import org.flasck.flas.parsedForm.TemplateCardReference;
import org.flasck.flas.parsedForm.TemplateCases;
import org.flasck.flas.parsedForm.TemplateDiv;
import org.flasck.flas.parsedForm.TemplateExplicitAttr;
import org.flasck.flas.parsedForm.TemplateFormat;
import org.flasck.flas.parsedForm.TemplateFormatEvents;
import org.flasck.flas.parsedForm.TemplateList;
import org.flasck.flas.parsedForm.TemplateOr;
import org.flasck.flas.parsedForm.TemplateReference;
import org.flasck.flas.parsedForm.TupleAssignment;
import org.flasck.flas.parsedForm.TupleMember;
import org.flasck.flas.parsedForm.TuplePattern;
import org.flasck.flas.parsedForm.TypeReference;
import org.flasck.flas.parsedForm.TypedPattern;
import org.flasck.flas.parsedForm.UnresolvedOperator;
import org.flasck.flas.parsedForm.UnresolvedVar;
import org.flasck.flas.parsedForm.VarPattern;
import org.flasck.flas.testrunner.UnitTests;
import org.flasck.flas.tokenizers.TemplateToken;
import org.zinutils.exceptions.UtilException;
import org.zinutils.utils.Indenter;

public class PFDumper {
	private void dumpRecursive(Indenter pw, Object obj) {
		if (obj == null) {
			pw.println("Error - null");
		} else if (obj instanceof String) { // I'm not sure I really believe in this case, but it came up
			pw.println("String: " + (String) obj);
		} else if (obj instanceof ContractDecl) {
			ContractDecl cd = (ContractDecl) obj;
			pw.print("cdecl " + cd.name().uniqueName());
			dumpPosition(pw, cd.kw, false);
			dumpLocation(pw, cd);
			dumpList(pw, cd.methods);
		} else if (obj instanceof ContractMethodDecl) {
			ContractMethodDecl cmd = (ContractMethodDecl) obj;
			pw.print((cmd.required?"required":"optional") + " " + cmd.dir.toString().toLowerCase() + " " + cmd.name.name);
			if (!cmd.required)
				dumpPosition(pw, cmd.rkw, false);
			dumpPosition(pw, cmd.dkw, false);
			dumpLocation(pw, cmd);
			dumpList(pw, cmd.args);
		} else if (obj instanceof ConstPattern) {
			ConstPattern cp = (ConstPattern) obj;
			pw.println(cp.type + ": " + cp.value);
		} else if (obj instanceof TypedPattern) {
			TypedPattern tp = (TypedPattern) obj;
			pw.print("[type] " + tp.var);
			dumpPosition(pw, tp.var.loc, true);
			dumpRecursive(pw.indent(), tp.type);
		} else if (obj instanceof VarPattern) {
			VarPattern tp = (VarPattern) obj;
			pw.print("[var] " + tp.var);
			dumpLocation(pw, tp);
		} else if (obj instanceof TuplePattern) {
			TuplePattern tp = (TuplePattern) obj;
			pw.println("[tuple]");
			dumpList(pw, tp.args);
		} else if (obj instanceof ConstructorMatch) {
			ConstructorMatch cm = (ConstructorMatch) obj;
			pw.print("[ctor] " + cm.ctor);
			dumpLocation(pw, cm);
			dumpList(pw, cm.args);
		} else if (obj instanceof ConstructorMatch.Field) {
			ConstructorMatch.Field cf = (ConstructorMatch.Field) obj;
			pw.print(cf.field);
			dumpLocation(pw, cf);
			dumpRecursive(pw.indent(), cf.patt);
		} else if (obj instanceof NumericLiteral) {
			NumericLiteral nl = (NumericLiteral) obj;
			pw.print("# " + nl.text);
			dumpLocation(pw, nl);
		} else if (obj instanceof StringLiteral) {
			StringLiteral sl = (StringLiteral) obj;
			pw.print("'' " + sl.text);
			dumpLocation(pw, sl);
		} else if (obj instanceof UnresolvedVar) {
			UnresolvedVar uv = (UnresolvedVar) obj;
			pw.print(uv.var);
			dumpLocation(pw, uv);
		} else if (obj instanceof UnresolvedOperator) {
			UnresolvedOperator uv = (UnresolvedOperator) obj;
			pw.print(uv.op);
			dumpLocation(pw, uv);
		} else if (obj instanceof ApplyExpr) {
			ApplyExpr ae = (ApplyExpr) obj;
			pw.print("<apply>");
			dumpLocation(pw, ae);
			dumpRecursive(pw.indent(), ae.fn);
			dumpList(pw, ae.args);
		} else if (obj instanceof IfExpr) {
			IfExpr ie = (IfExpr) obj;
			pw.println("if " + ie.guard.toString());
			dumpRecursive(pw.indent(), ie.ifExpr);
			if (ie.elseExpr != null) {
				pw.println("else");
				dumpRecursive(pw.indent(), ie.elseExpr);
			}
		} else if (obj instanceof CastExpr) {
			CastExpr ce = (CastExpr) obj;
			pw.print("cast " + ce.castTo);
			dumpPosition(pw, ce.ctLoc, true);
			dumpRecursive(pw.indent(), ce.expr);
		} else if (obj instanceof FunctionCaseDefn) {
			FunctionCaseDefn fcd = (FunctionCaseDefn) obj;
			pw.print(fcd.intro.name().uniqueName());
			dumpLocation(pw, fcd);
			dumpList(pw, fcd.intro.args);
			pw.println(" =");
			dumpRecursive(pw.indent(), fcd.expr);
			dumpScope(pw, null /*fcd.innerScope()*/);
		} else if (obj instanceof CardDefinition) {
			CardDefinition cd = (CardDefinition) obj;
			pw.print("card " + cd.cardName.uniqueName());
			dumpPosition(pw, cd.kw, false);
			dumpLocation(pw, cd);
			if (cd.state != null)
				dumpRecursive(pw.indent(), cd.state);
			for (Template t : cd.templates)
				dumpRecursive(pw.indent(), t);
			dumpList(pw, cd.d3s);
			dumpList(pw, cd.contracts);
			dumpList(pw, cd.handlers);
			dumpList(pw, cd.services);
			dumpScope(pw, null /* cd.innerScope() */);
		} else if (obj instanceof StateDefinition) {
			StateDefinition sd = (StateDefinition) obj;
			pw.print("state");
			dumpLocation(pw, sd);
			dumpList(pw, sd.fields);
		} else if (obj instanceof StructDefn) {
			StructDefn sd = (StructDefn) obj;
			pw.print(sd.type.name().toLowerCase() + " " + sd.name().uniqueName() + polys(sd.polys()));
			dumpPosition(pw, sd.kw, false);
			dumpPosition(pw, sd.location(), false);
			for (PolyType p : sd.polys())
				dumpPosition(pw, p.location(), false);
			pw.println("");
			dumpList(pw, sd.fields);
		} else if (obj instanceof ObjectDefn) {
			ObjectDefn od = (ObjectDefn) obj;
			pw.print("object " + od.name().uniqueName() + polys(od.polys()));
			dumpPosition(pw, od.kw, false);
			dumpPosition(pw, od.location(), false);
			for (PolyType p : od.polys())
				dumpPosition(pw, p.location(), false);
			pw.println("");
//			for (ObjectMethod om : od.methods)
//				dumpRecursive(pw.indent(), om.getMethod());
		} else if (obj instanceof StructField) {
			StructField sf = (StructField) obj;
			pw.print(sf.name);
			dumpLocation(pw, sf);
			dumpRecursive(pw.indent(), sf.type);
			if (sf.init != null) {
				pw.print(" <-");
				dumpPosition(pw, sf.assOp, true);
				dumpRecursive(pw.indent(), sf.init);
			}
		} else if (obj instanceof ContractImplements) {
			ContractImplements ctr = (ContractImplements) obj;
			pw.print("implements " + ctr.name() + (ctr.referAsVar != null ? " " + ctr.referAsVar : ""));
			dumpPosition(pw, ctr.kw, false);
			dumpPosition(pw, ctr.location(), ctr.referAsVar == null);
			if (ctr.referAsVar != null)
				dumpPosition(pw, ctr.varLocation, true);
			dumpList(pw, ctr.methods);
		} else if (obj instanceof ContractService) {
			ContractService ctr = (ContractService) obj;
			pw.print("service " + ctr.name() + (ctr.referAsVar != null ? " " + ctr.referAsVar : ""));
			dumpPosition(pw, ctr.kw, false);
			dumpPosition(pw, ctr.location(), ctr.referAsVar == null);
			if (ctr.referAsVar != null)
				dumpPosition(pw, ctr.vlocation, true);
			dumpList(pw, ctr.methods);
		} else if (obj instanceof HandlerImplements) {
			HandlerImplements hi = (HandlerImplements) obj;
			pw.print("handler " + hi.name() + " " + hi.handlerName.uniqueName() + " (" + (hi.inCard?"card":"free") + ")");
			dumpPosition(pw, hi.kw, false);
			dumpPosition(pw, hi.typeLocation, false);
			dumpLocation(pw, hi);
			dumpList(pw, hi.boundVars);
			dumpList(pw, hi.methods);
		} else if (obj instanceof MethodCaseDefn) {
			MethodCaseDefn mcd = (MethodCaseDefn) obj;
			pw.print("method " + mcd.caseName().uniqueName());
			dumpLocation(pw, mcd);
			dumpList(pw, mcd.intro.args);
			dumpList(pw, mcd.messages);
			dumpScope(pw, mcd.innerScope());
		} else if (obj instanceof EventCaseDefn) {
			EventCaseDefn ecd = (EventCaseDefn) obj;
			pw.print("event " + ecd.caseName().uniqueName());
			dumpPosition(pw, ecd.kw, false);
			dumpLocation(pw, ecd);
			dumpList(pw, ecd.intro.args);
			dumpList(pw, ecd.messages);
			dumpScope(pw, ecd.innerScope());
		} else if (obj instanceof MethodMessage) {
			MethodMessage mm = (MethodMessage) obj;
			if (mm.slot != null) {
				pw.print("assign " + slotName(mm.slot) + " <-");
				for (Locatable x : mm.slot) {
					dumpPosition(pw, x.location(), false);
				}
			} else {
				pw.print("invoke");
			}
			dumpPosition(pw, mm.kw, true);
			dumpRecursive(pw.indent(), mm.expr);
		} else if (obj instanceof Template) {
			Template t = (Template) obj;
			pw.print("template" + (t.name.baseName() != null ? " " + t.name.baseName() : ""));
			dumpPosition(pw, t.kw, false);
			dumpPosition(pw, t.location(), false);
			for (LocatedToken a : t.args) {
				pw.print(" " + a.text);
			}
			for (LocatedToken a : t.args) {
				dumpPosition(pw, a.location, false);
			}
			pw.newline();
			if (t.content != null)
				dumpRecursive(pw.indent(), t.content);
		} else if (obj instanceof TemplateDiv) {
			TemplateDiv td = (TemplateDiv) obj;
			pw.print(".");
			if (td.customTagLoc != null) // cannot test the var because we hack in "li"
				pw.print(" " + td.customTag);
			else if (td.customTagVar != null)
				pw.print(" " + td.customTagVar);
			dumpPosition(pw, td.kw, false);
			if (td.customTagLoc != null)
				dumpPosition(pw, td.customTagLoc, false);
			else if (td.customTagVar != null)
				dumpPosition(pw, td.customTagVarLoc, false);
			pw.newline();
			dumpList(pw, td.attrs);
			dumpList(pw, td.nested);
			dumpList(pw, td.webzipBlocks);
		} else if (obj instanceof TemplateExplicitAttr) {
			TemplateExplicitAttr attr = (TemplateExplicitAttr) obj;
			pw.print("attr " + attr.attr);
			dumpPosition(pw, attr.location, true);
			dumpRecursive(pw.indent(), attr.value);
		} else if (obj instanceof TemplateList) {
			TemplateList td = (TemplateList) obj;
			pw.print("+ " + td.listExpr);
			if (td.iterVar != null)
				pw.print(" " + td.iterVar);
			dumpPosition(pw, td.kw, false);
			dumpPosition(pw, td.listLoc, td.iterVar == null);
			if (td.iterVar != null)
				dumpPosition(pw, td.iterLoc, true);
			dumpRecursive(pw.indent(), td.template);
		} else if (obj instanceof TemplateReference) {
			TemplateReference tr = (TemplateReference) obj;
			pw.print(tr.name);
			dumpLocation(pw, tr);
			dumpList(pw, tr.args);
		} else if (obj instanceof TemplateCardReference) {
			TemplateCardReference tr = (TemplateCardReference) obj;
			if (tr.explicitCard != null)
				pw.print("Explicit " + tr.explicitCard);
			else
				pw.print("Yoyo "+ tr.yoyoVar);
			dumpLocation(pw, tr);
		} else if (obj instanceof TemplateCases) {
			TemplateCases tc = (TemplateCases) obj;
			pw.print("Cases");
			dumpLocation(pw, tc);
			dumpRecursive(pw.indent(), tc.switchOn);
			dumpList(pw, tc.cases);
		} else if (obj instanceof TemplateOr) {
			TemplateOr tor = (TemplateOr) obj;
			if (tor.cond != null)
				pw.print("Or");
			else
				pw.print("Else");
			dumpLocation(pw, tor);
			if (tor.cond != null)
				dumpRecursive(pw.indent(), tor.cond);
			dumpRecursive(pw.indent(), tor.template);
		} else if (obj instanceof ContentString) {
			ContentString ce = (ContentString) obj;
			pw.print("'' " + ce.text);
			dumpPosition(pw, ce.kw, true);
		} else if (obj instanceof ContentExpr) {
			ContentExpr ce = (ContentExpr) obj;
			pw.print("<cexpr>");
			dumpPosition(pw, ce.kw, true);
			dumpRecursive(pw.indent(), ce.expr);
		} else if (obj instanceof D3Thing) {
			D3Thing d3 = (D3Thing) obj;
			dumpRecursive(pw, d3.d3);
			dumpList(pw, d3.patterns);
		} else if (obj instanceof D3Intro) {
			D3Intro d3 = (D3Intro) obj;
			pw.print("d3 " + d3.name + " " + d3.iterVar.var);
			dumpPosition(pw, d3.kw, false);
			dumpPosition(pw, d3.nameLoc, false);
			dumpPosition(pw, d3.varLoc, true);
			dumpRecursive(pw.indent(), d3.expr);
		} else if (obj instanceof D3PatternBlock) {
			D3PatternBlock blk = (D3PatternBlock) obj;
			pw.print("Pattern " + blk.pattern.text);
			dumpPosition(pw, blk.kw, false);
			dumpPosition(pw, blk.pattern.location, true);
			for (D3Section x : blk.sections)
				dumpRecursive(pw.indent(), x);
		} else if (obj instanceof D3Section) {
			D3Section s = (D3Section) obj;
			pw.print(s.name);
			dumpLocation(pw, s);
			dumpList(pw, s.properties);
			dumpList(pw, s.actions);
		} else if (obj instanceof PropertyDefn) {
			PropertyDefn d = (PropertyDefn) obj;
			pw.print(d.name);
			dumpLocation(pw, d);
			dumpRecursive(pw, d.value);
		} else if (obj instanceof TemplateToken) {
			// used in formats at least
			TemplateToken tt = (TemplateToken) obj;
			pw.print("format ");
			if (tt.type == TemplateToken.STRING) {
				pw.print("'' " + tt.text);
				dumpPosition(pw, tt.location, true);
			} else if (tt.type == TemplateToken.IDENTIFIER) {
				pw.print(tt.text);
				dumpPosition(pw, tt.location, true);
			} else
				throw new UtilException("Can't handle template token " + tt.type);
		} else if (obj instanceof EventHandler) {
			EventHandler eh = (EventHandler) obj;
			pw.print("=>");
			dumpPosition(pw, eh.kw, true);
			Indenter p2 = pw.indent();
			p2.print(eh.action);
			dumpPosition(p2, eh.actionPos, true);
			dumpRecursive(pw.indent(), eh.expr);
		} else if (obj instanceof FunctionTypeReference) {
			FunctionTypeReference t = (FunctionTypeReference) obj;
			pw.print(t.name());
			dumpLocation(pw, t);
			Indenter ind = pw.indent();
			for (TypeReference a : t.args)
				dumpRecursive(ind, a);
		} else if (obj instanceof TupleMember) {
			TupleMember t = (TupleMember) obj;
			LocatedName locatedName = t.ta.vars.get(t.which);
			pw.println(locatedName.text);
			dumpRecursive(pw.indent(), t.ta);
		} else if (obj instanceof TupleAssignment) {
			TupleAssignment t = (TupleAssignment) obj;
			pw.println("(" + t.vars + ")");
			dumpRecursive(pw.indent(), t.expr);
		} else if (obj instanceof TypeReference) {
			TypeReference t = (TypeReference) obj;
			pw.print(t.name());
			dumpLocation(pw, t);
			if (t.hasPolys()) {
				Indenter ind = pw.indent();
				for (TypeReference p : t.polys())
					dumpRecursive(ind, p);
			}
		} else if (obj instanceof UnitTests) {
			dumpScope(pw.indent(), ((UnitTests)obj).scope());
		} else
			throw new UtilException("Cannot handle dumping " + obj.getClass());
		if (obj instanceof TemplateFormat) {
			TemplateFormat tf = (TemplateFormat) obj;
			dumpList(pw, tf.formats);
			if (obj instanceof TemplateFormatEvents) {
				TemplateFormatEvents tfe = (TemplateFormatEvents) tf;
				dumpList(pw, tfe.handlers);
			}
		}
	}

	private void dumpLocation(Indenter pw, Locatable obj) {
		dumpPosition(pw, obj.location(), true);
	}

	private void dumpPosition(Indenter pw, InputPosition pos, boolean withNL) {
		if (pos == null) {
			pw.print(" @{null}");
		} else {
			pw.print(" @{" + pos.lineNo + ":" + pos.off + "|" + pos.asToken() + "}");
		}
		if (withNL)
			pw.println("");
	}

	private static String slotName(List<Locatable> slot) {
		StringBuilder ret = new StringBuilder();
		for (Locatable s : slot) {
			LocatedToken t = (LocatedToken) s;
			if (ret.length() > 0)
				ret.append(".");
			ret.append(t.text);
		}
		return ret.toString();
	}

	private String polys(final List<PolyType> polys) {
		if (polys == null || polys.isEmpty())
			return "";
		StringBuilder sb = new StringBuilder();
		for (PolyType tr : polys) {
			sb.append(" ");
			sb.append(tr.name());
		}
		return sb.toString();
	}

	public void dumpScope(Indenter pw, IScope s) {
		Indenter pi = pw.indent();
		for (ScopeEntry k : s) {
			dumpRecursive(pi, k.getValue());
		}
		pi.flush();
	}

	protected void dumpList(Indenter pw, List<?> objs) {
		Indenter pi = pw.indent();
		for (Object x : objs)
			dumpRecursive(pi, x);
	}
}
