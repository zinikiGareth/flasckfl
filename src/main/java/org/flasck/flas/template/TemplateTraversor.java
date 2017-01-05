package org.flasck.flas.template;

import java.util.ArrayList;
import java.util.List;

import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.commonBase.ApplyExpr;
import org.flasck.flas.commonBase.IfExpr;
import org.flasck.flas.commonBase.Locatable;
import org.flasck.flas.commonBase.NumericLiteral;
import org.flasck.flas.commonBase.StringLiteral;
import org.flasck.flas.commonBase.names.AreaName;
import org.flasck.flas.commonBase.names.SolidName;
import org.flasck.flas.commonBase.template.TemplateListVar;
import org.flasck.flas.jsform.JSTarget;
import org.flasck.flas.rewriter.Rewriter;
import org.flasck.flas.rewrittenForm.CardFunction;
import org.flasck.flas.rewrittenForm.CardMember;
import org.flasck.flas.rewrittenForm.LocalVar;
import org.flasck.flas.rewrittenForm.PackageVar;
import org.flasck.flas.rewrittenForm.RWContentExpr;
import org.flasck.flas.rewrittenForm.RWContentString;
import org.flasck.flas.rewrittenForm.RWD3Thing;
import org.flasck.flas.rewrittenForm.RWEventHandler;
import org.flasck.flas.rewrittenForm.RWFunctionCaseDefn;
import org.flasck.flas.rewrittenForm.RWFunctionDefinition;
import org.flasck.flas.rewrittenForm.RWStructDefn;
import org.flasck.flas.rewrittenForm.RWTemplate;
import org.flasck.flas.rewrittenForm.RWTemplateCardReference;
import org.flasck.flas.rewrittenForm.RWTemplateCases;
import org.flasck.flas.rewrittenForm.RWTemplateDiv;
import org.flasck.flas.rewrittenForm.RWTemplateExplicitAttr;
import org.flasck.flas.rewrittenForm.RWTemplateFormat;
import org.flasck.flas.rewrittenForm.RWTemplateFormatEvents;
import org.flasck.flas.rewrittenForm.RWTemplateLine;
import org.flasck.flas.rewrittenForm.RWTemplateList;
import org.flasck.flas.rewrittenForm.RWTemplateOr;
import org.flasck.flas.tokenizers.TemplateToken;
import org.zinutils.exceptions.UtilException;

public class TemplateTraversor {
	public class DefinedVar {
		final String name;
		final AreaName definedIn;
		
		public DefinedVar(String name, AreaName area) {
			this.name = name;
			this.definedIn = area;
		}
	}

	public class GeneratorContext {
		private String introduceVarHere;
		private final List<DefinedVar> varsToCopy = new ArrayList<DefinedVar>();

		public GeneratorContext() {
		}
		
		public void varToCopy(String s, AreaName area) {
			varsToCopy.add(new DefinedVar(s, area));
		}
		
		public void removeLastCopyVar() {
			varsToCopy.remove(varsToCopy.size()-1);
		}

		public void newVar(String tlv) {
			introduceVarHere = tlv;
		}

		public String extractNewVar() {
			String ret = introduceVarHere;
			introduceVarHere = null;
			return ret;
		}
	}

	private final Rewriter rewriter;
	private List<TemplateGenerator> tgs;
	InputPosition posn = new InputPosition("template", 1, 1, "");
	private final Object nil;
	private final Object cons;

	public TemplateTraversor(Rewriter rewriter, List<TemplateGenerator> tgs) {
		this.rewriter = rewriter;
		this.tgs = tgs;
		this.nil = rewriter.getMe(posn, new SolidName(null, "Nil"));
		this.cons = rewriter.getMe(posn, new SolidName(null, "Cons"));
	}

	public void generate(Rewriter rw, JSTarget target) {
		for (RWTemplate cg : rewriter.templates)
			generateTemplate(rw, cg);
	}

	private void generateTemplate(Rewriter rw, RWTemplate cg) {
		GeneratorContext cx = new GeneratorContext();
		AreaName areaName = null;
		if (cg != null && cg.content != null)
			areaName = cg.areaName();
		for (TemplateGenerator tg : tgs)
			tg.generateRender(cg.tname, areaName);
		recurse(cx, areaName, cg.content, null);
	}

	private List<AreaGenerator> recurse(GeneratorContext cx, AreaName areaName, RWTemplateLine tl, AreaName parentArea) {
		if (tl == null)
			return null;
		String base;
		boolean isEditable = false;
		String customTag = null;
		String nsTag = null;
		Object wantCard = null;
		Object wantYoyo = null;
		if (tl instanceof RWTemplateDiv) {
			RWTemplateDiv td = (RWTemplateDiv) tl;
			base = "DivArea";
			if (td.customTag != null) {
				customTag = td.customTag;
				if (td.customTag.equals("svg"))
					nsTag = "'http://www.w3.org/2000/svg'";
			}
			// TODO: a variable custom tag is hard & needs "assign" logic
		} else if (tl instanceof RWTemplateList) {
			RWTemplateList ul = (RWTemplateList) tl;
			base = "ListArea";
			if (ul.customTag != null) {
				customTag = ul.customTag;
				if (ul.customTag.equals("svg"))
					nsTag = "'http://www.w3.org/2000/svg'";
			}
			// TODO: a variable custom tag is hard & needs "assign" logic
		} else if (tl instanceof RWContentString || tl instanceof RWContentExpr) {
			base = "TextArea";
			isEditable = tl instanceof RWContentExpr && ((RWContentExpr)tl).editable();
		} else if (tl instanceof RWTemplateCardReference) {
			RWTemplateCardReference cr = (RWTemplateCardReference) tl;
			base = "CardSlotArea";
			if (cr.explicitCard != null)
				wantCard = cr.explicitCard;
			else if (cr.yoyoVar != null) {
				wantYoyo = cr.yoyoVar;
			} else
				throw new UtilException("Can't handle this case");
		} else if (tl instanceof RWTemplateCases) {
			base = "CasesArea";
		} else if (tl instanceof RWD3Thing) {
			base = "D3Area";
		} else {
			throw new UtilException("Template of type " + (tl == null ? "null":tl.getClass()) + " not supported");
		}
		List<AreaGenerator> areas = new ArrayList<AreaGenerator>();
		for (TemplateGenerator tg : tgs)
			areas.add(tg.area(areaName, base, customTag, nsTag, wantCard, wantYoyo));
		for (DefinedVar vc : cx.varsToCopy) {
			String s = vc.name;
			for (AreaGenerator area : areas)
				area.copyVar(parentArea, vc.definedIn, s);
		}
		String newVar = cx.extractNewVar();
		if (newVar != null) {
			cx.varToCopy(newVar, areaName);
			for (AreaGenerator area : areas)
				area.assignToVar(newVar);
		}
		if (tl instanceof RWTemplateDiv) {
			RWTemplateDiv td = (RWTemplateDiv) tl;
			int an = 1;
			for (Object a : td.attrs) {
				if (a instanceof RWTemplateExplicitAttr) {
					RWTemplateExplicitAttr tea = (RWTemplateExplicitAttr) a;
					for (AreaGenerator area : areas)
						area.handleTEA(tea, an);
					String saf = areaName.jsName() + ".prototype._setAttr_" + an;
					callOnAssign(areas, tea.value, saf, true, null);
					an++;
				} else
					throw new UtilException("Cannot handle attr " + a.getClass());
			}
			if (td.droppables != null) {
				for (AreaGenerator area : areas)
					area.dropZone(td.droppables);
			}
			for (RWTemplateLine c : td.nested) {
				AreaName cn = c.areaName();
				int idx = cn.jsName().lastIndexOf(".B")+2;
				String v = 'b'+cn.jsName().substring(idx);
				for (AreaGenerator area : areas)
					area.createNested(v, cn);
				recurse(cx, cn, c, areaName);
			}
		} else if (tl instanceof RWTemplateList) {
			RWTemplateList l = (RWTemplateList) tl;
			TemplateListVar lv = (TemplateListVar)l.iterVar;
			String tlv = lv == null ? null : lv.simpleName;
			if (l.supportDragOrdering) {
				for (AreaGenerator area : areas)
					area.supportDragging();
			}
			AreaName item = l.template.areaName();
			for (AreaGenerator area : areas)
				area.newListChild(item);
			if (tlv != null)
				cx.newVar(tlv);
			List<AreaGenerator> cfn = recurse(cx, item, l.template, areaName);
			if (l.supportDragOrdering) {
				for (AreaGenerator area : cfn)
					area.makeItemDraggable();
			}
			for (AreaGenerator area : areas)
				area.assignToList(l.listFn);
			callOnAssign(areas, l.listVar, areaName.jsName() + ".prototype._assignToVar", false, "lv");
		} else if (tl instanceof RWContentString) {
			RWContentString cs = (RWContentString) tl;
			for (AreaGenerator area : areas)
				area.setText(cs.text);
		} else if (tl instanceof RWContentExpr) {
			RWContentExpr ce = (RWContentExpr)tl;
			Object valExpr = ce.expr;
			callOnAssign(areas, valExpr, areaName.jsName() + ".prototype._contentExpr", true, null);

			String tfn = ce.fnName.name;
			for (AreaGenerator area : areas)
				area.contentExpr(tfn, ce.rawHTML);
			
			if (isEditable) {
				// for it to be editable, it must be a clear field of a clear object
				if (valExpr instanceof CardMember) {
					CardMember cm = (CardMember) valExpr;
					for (AreaGenerator area : areas)
						area.makeEditable(ce, cm.var);
				} else if (valExpr instanceof ApplyExpr) {
					ApplyExpr ae = (ApplyExpr) valExpr;
					if (!(ae.fn instanceof PackageVar) || !((PackageVar)ae.fn).uniqueName().equals("FLEval.field"))
						throw new UtilException("Cannot edit: " + ae);
					for (AreaGenerator area : areas)
						area.makeEditable(ce, ((StringLiteral)ae.args.get(1)).text);
				} else 
					throw new UtilException("Cannot edit: " + valExpr);
			}
		} else if (tl instanceof RWTemplateCardReference) {
			RWTemplateCardReference cr = (RWTemplateCardReference) tl;
			if (cr.explicitCard != null)
				; // fully handled above
			else if (cr.yoyoVar != null) {
				Object valExpr = cr.yoyoVar;
				callOnAssign(areas, valExpr, areaName.jsName() + ".prototype._yoyoExpr", true, null);
	
				String tfn = simpleName(cr.fnName);
				for (AreaGenerator area : areas)
					area.yoyoExpr(tfn);
			} else
				throw new UtilException("handle this case");
		} else if (tl instanceof RWTemplateCases) {
			RWTemplateCases tc = (RWTemplateCases) tl;
			String sn = areaName.jsName() + ".prototype._chooseCase";
			List<CaseChooser> ccs = new ArrayList<CaseChooser>();
			for (AreaGenerator area : areas)
				ccs.add(area.chooseCase(sn));
			callOnAssign(areas, tc.switchOn, sn, true, null);

			for (RWTemplateOr oc : tc.cases) {
				AreaName cn = oc.areaName();

				List<CaseChooser> branches = new ArrayList<CaseChooser>();

				if (oc.cond == null) {
					branches.addAll(ccs);
				} else {
					String tfn = simpleName(oc.fnName);
					for (CaseChooser cc : ccs)
						branches.add(cc.handleCase(tfn));
				}
				for (CaseChooser cc : branches)
					cc.code(cn);
				recurse(cx, cn, oc.template, areaName);
				if (oc.cond != null)
					callOnAssign(areas, oc.cond, sn, false, null);
			}
		} else if (tl instanceof RWD3Thing) {
			RWD3Thing d3 = (RWD3Thing) tl;
			callOnAssign(areas, d3.data, "D3Area.prototype._onUpdate", false, null);
		} else {
			throw new UtilException("Template of type " + tl.getClass() + " not supported");
		}
		if (tl instanceof RWTemplateFormat) {
			handleFormatsAndEvents(cx, areas, areaName, isEditable, (RWTemplateFormat)tl);
		}
		if (newVar != null) {
			cx.removeLastCopyVar();
		}
		for (AreaGenerator area : areas)
			area.done();
		return areas;
	}

	protected void handleFormatsAndEvents(GeneratorContext cx, List<AreaGenerator> areas, AreaName areaName, boolean isEditable, RWTemplateFormat tl) {
		StringBuilder simple = new StringBuilder();
		if (isEditable)
			simple.append(" flasck-editable");
		Object expr = null;
		InputPosition first = null;
		for (Object o : tl.formats) {
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
			String scf = areaName.jsName() + ".prototype._setVariableFormats";
			String tfn = tl.dynamicFunction.name;
			for (AreaGenerator area : areas)
				area.setVarFormats(tfn);
			callOnAssign(areas, expr, scf, true, null);
		}
		else if (expr == null && simple.length() > 0) {
			for (AreaGenerator area : areas)
				area.setSimpleClass(simple.substring(1));
		}
		if (tl instanceof RWTemplateFormatEvents) {
			RWTemplateFormatEvents tfe = (RWTemplateFormatEvents) tl;
			if (!tfe.handlers.isEmpty()) {
				List<EventHandlerGenerator> ehgs = new ArrayList<>();
				for (AreaGenerator area : areas)
					ehgs.add(area.needAddHandlers());
				boolean isFirst = true;
				for (RWEventHandler eh : tfe.handlers) {
					String tfn = eh.handlerFn.name;

					// add a hack to allow us to NOT overwrite events that we want to intercept first
					boolean distinguish = false;
					if (eh.action.equals("drop"))
						distinguish = true;
					for (EventHandlerGenerator ehg : ehgs)
						ehg.handle(distinguish, eh.action, tfn);
					callOnAssign(areas, eh.expr, areaName.jsName() + ".prototype._add_handlers", isFirst, null);
					isFirst = false;
				}
			}
		}
	}
	
	protected void callOnAssign(List<AreaGenerator> areas, Object valExpr, String call, boolean addAssign, String passVar) {
		if (valExpr instanceof CardMember) {
			for (AreaGenerator area : areas)
				area.onAssign((CardMember)valExpr, call);
		} else if (valExpr instanceof TemplateListVar) {
			String var = ((TemplateListVar)valExpr).simpleName;
			for (AreaGenerator area : areas)
				area.interested(var, call);
		} else if (valExpr instanceof CardFunction) {
			// we need to track down the function (if it's not in the object already) and callOnAssign it's definition
			CardFunction cf = (CardFunction) valExpr;
			String fullName = cf.clzName + "." + cf.function;
			RWFunctionDefinition fd = rewriter.functions.get(fullName);
			if (fd != null)
				for (RWFunctionCaseDefn fcd : fd.cases)
					callOnAssign(areas, fcd.expr, call, false, passVar);
		} else if (valExpr instanceof LocalVar || valExpr instanceof StringLiteral || valExpr instanceof NumericLiteral || valExpr instanceof PackageVar || valExpr instanceof RWStructDefn) {
			// nothing to do here, not variable
		} else if (valExpr instanceof ApplyExpr) {
			ApplyExpr ae = (ApplyExpr) valExpr;
			if (ae.fn instanceof PackageVar && ((PackageVar)ae.fn).id.equals("FLEval.field")) {
				Object expr = ae.args.get(0);
				String field = ((StringLiteral)ae.args.get(1)).text;
				if (expr instanceof TemplateListVar) {
					callOnAssign(areas, expr, call, false, passVar);
					for (AreaGenerator area : areas)
						area.onFieldAssign(expr, field, call);
				} else if (expr instanceof CardMember) {
					// need to handle if the whole member gets assigned
					callOnAssign(areas, expr, call, false, passVar);
					// also handle if this field gets assigned
					for (AreaGenerator area : areas)
						area.onFieldAssign(expr, field, call);
				} else {
					// This includes the case where we have delegated knowledge of our state to some other function.
					// It needs to interact with the parent through some kind of dependency analysis to identify
					// what state elements have changed, in particular if presented with a local variable, what that would signify.
					
					// I think there are two routes to get here:
					//  - you have a complicated expression in the "main" function
					//  - you have a local variable in a subsidiary function (see CardFunction case above)
					
					// I think what we need to do is to pass down when we call with regard to card functions a set of the expressions we're passing down
					// and then down here to traverse the ApplyExpr's and if we come across a local variable to look up an expr in that map and re-traverse it ...
					System.out.println("There is an update case here with regard to local variables that I don't really understand how to process");
					System.out.println("  -> " + expr);
					return;
				}
			} else {
				callOnAssign(areas, ae.fn, call, false, passVar);
				for (Object o : ae.args)
					callOnAssign(areas, o, call, false, passVar);
			}
		} else if (valExpr instanceof IfExpr) {
			IfExpr ie = (IfExpr) valExpr;
			callOnAssign(areas, ie.guard, call, false, passVar);
			callOnAssign(areas, ie.ifExpr, call, false, passVar);
			callOnAssign(areas, ie.elseExpr, call, false, passVar);
		} else
			throw new UtilException("Not handled: " + valExpr.getClass());
		if (addAssign) {
			for (AreaGenerator area : areas)
				area.addAssign(call, passVar);
		}
	}

	@Deprecated
	public static String simpleName(String name) {
		int idx = name.lastIndexOf(".");
		if (idx == -1)
			throw new UtilException("No . in " + name);
		return name.substring(idx+1);
	}
}
