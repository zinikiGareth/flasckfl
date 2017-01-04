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
import org.flasck.flas.commonBase.names.StructName;
import org.flasck.flas.commonBase.template.TemplateListVar;
import org.flasck.flas.jsform.JSForm;
import org.flasck.flas.jsform.JSTarget;
import org.flasck.flas.jsgen.Generator;
import org.flasck.flas.jsgen.JSAreaGenerator;
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
import org.zinutils.bytecode.Expr;
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

		private final JSTarget target;
		private final String protoName;
		private String introduceVarHere;
		private final List<DefinedVar> varsToCopy = new ArrayList<DefinedVar>();
		private final Object nil;
		private final Object cons;
		private final String javaName;

		public GeneratorContext(JSTarget target, Rewriter rw, RWTemplate cg) {
			this.target = target;
			this.javaName = cg.prefix;
			InputPosition posn = new InputPosition("template", 1, 1, "");
			this.protoName = Generator.lname(cg.prefix, true);
			this.nil = rw.getMe(posn, new StructName(null, "Nil"));
			this.cons = rw.getMe(posn, new StructName(null, "Cons"));
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
	private final TemplateGenerator dg;
	private final TemplateGenerator jsg;
	private List<TemplateGenerator> tgs;

	public TemplateTraversor(Rewriter rewriter, List<TemplateGenerator> tgs) {
		this.rewriter = rewriter;
		this.tgs = tgs;
		dg = tgs.get(0);
		jsg = tgs.get(1);
	}

	public void generate(Rewriter rw, JSTarget target) {
		for (RWTemplate cg : rewriter.templates)
			generateTemplate(rw, target, cg);
	}

	private void generateTemplate(Rewriter rw, JSTarget target, RWTemplate cg) {
		GeneratorContext cx = new GeneratorContext(target, rw, cg);
		AreaName areaName = null;
		if (cg != null && cg.content != null)
			areaName = cg.areaName();
		for (TemplateGenerator tg : tgs)
			tg.generateRender(cx.javaName, areaName);
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
		List<AreaGenerator> ret = new ArrayList<AreaGenerator>();
		AreaGenerator area = dg.area(areaName, base, customTag, nsTag, wantCard, wantYoyo);
		JSAreaGenerator jsArea = (JSAreaGenerator) jsg.area(areaName, base, customTag, nsTag, wantCard, wantYoyo);
		ret.add(area);
		ret.add(jsArea);
		JSForm fn = jsArea.fn;
		for (DefinedVar vc : cx.varsToCopy) {
			String s = vc.name;
			jsArea.copyVar(parentArea, vc.definedIn, s);
			area.copyVar(parentArea, vc.definedIn, s);
		}
		String newVar = cx.extractNewVar();
		if (newVar != null) {
			cx.varToCopy(newVar, areaName);
			jsArea.assignToVar(newVar);
			area.assignToVar(newVar);
		}
		if (tl instanceof RWTemplateDiv) {
			RWTemplateDiv td = (RWTemplateDiv) tl;
			int an = 1;
			for (Object a : td.attrs) {
				if (a instanceof RWTemplateExplicitAttr) {
					RWTemplateExplicitAttr tea = (RWTemplateExplicitAttr) a;
					area.handleTEA(tea, an);
					jsArea.handleTEA(tea, an);
					String saf = areaName.jsName() + ".prototype._setAttr_" + an;
					callOnAssign(fn, tea.value, area, saf, true, null);
					an++;
				} else
					throw new UtilException("Cannot handle attr " + a.getClass());
			}
			if (td.droppables != null) {
				area.dropZone(td.droppables);
				jsArea.dropZone(td.droppables);
			}
			for (RWTemplateLine c : td.nested) {
				AreaName cn = c.areaName();
				int idx = cn.jsName().lastIndexOf(".B")+2;
				String v = 'b'+cn.jsName().substring(idx);
				jsArea.createNested(v, cn);
				area.createNested(v, cn);
				recurse(cx, cn, c, areaName);
			}
		} else if (tl instanceof RWTemplateList) {
			RWTemplateList l = (RWTemplateList) tl;
			TemplateListVar lv = (TemplateListVar)l.iterVar;
			String tlv = lv == null ? null : lv.simpleName;
			if (l.supportDragOrdering) {
				area.supportDragging();
				jsArea.supportDragging();
			}
			AreaName item = l.template.areaName();
			jsArea.newListChild(item);
			area.newListChild(item);
			if (tlv != null)
				cx.newVar(tlv);
			List<AreaGenerator> cfn = recurse(cx, item, l.template, areaName);
			if (l.supportDragOrdering) {
				for (AreaGenerator ag : cfn)
					ag.makeItemDraggable();
			}
			area.assignToList(l.listFn);
			jsArea.assignToList(l.listFn);
			callOnAssign(fn, l.listVar, area, areaName.jsName() + ".prototype._assignToVar", false, "lv");
		} else if (tl instanceof RWContentString) {
			RWContentString cs = (RWContentString) tl;
			jsArea.setText(cs.text);
			area.setText(cs.text);
		} else if (tl instanceof RWContentExpr) {
			RWContentExpr ce = (RWContentExpr)tl;
			Object valExpr = ce.expr;
			callOnAssign(fn, valExpr, area, areaName.jsName() + ".prototype._contentExpr", true, null);

			String tfn = simpleName(ce.fnName);
			jsArea.contentExpr(tfn, ce.rawHTML);
			area.contentExpr(tfn, ce.rawHTML);
			
			if (isEditable) {
				// for it to be editable, it must be a clear field of a clear object
				if (valExpr instanceof CardMember) {
					CardMember cm = (CardMember) valExpr;
					area.makeEditable();
					jsArea.makeEditable();
					createRules(cx, ce, areaName, null, cm.var);
				} else if (valExpr instanceof ApplyExpr) {
					ApplyExpr ae = (ApplyExpr) valExpr;
					if (!(ae.fn instanceof PackageVar) || !((PackageVar)ae.fn).uniqueName().equals("FLEval.field"))
						throw new UtilException("Cannot edit: " + ae);
					area.makeEditable();
					jsArea.makeEditable();
					createRules(cx, ce, areaName, ae.args.get(0), ((StringLiteral)ae.args.get(1)).text);
				} else 
					throw new UtilException("Cannot edit: " + valExpr);
			}
		} else if (tl instanceof RWTemplateCardReference) {
			RWTemplateCardReference cr = (RWTemplateCardReference) tl;
			if (cr.explicitCard != null)
				; // fully handled above
			else if (cr.yoyoVar != null) {
				Object valExpr = cr.yoyoVar;
				callOnAssign(fn, valExpr, area, areaName.jsName() + ".prototype._yoyoExpr", true, null);
	
				String tfn = simpleName(cr.fnName);
				jsArea.yoyoExpr(tfn);
				area.yoyoExpr(tfn);
			} else
				throw new UtilException("handle this case");
		} else if (tl instanceof RWTemplateCases) {
			RWTemplateCases tc = (RWTemplateCases) tl;
			String sn = areaName.jsName() + ".prototype._chooseCase";
			CaseChooser cc = jsArea.chooseCase(sn);
			CaseChooser dcc = area.chooseCase(sn);
			callOnAssign(fn, tc.switchOn, area, sn, true, null);

			for (RWTemplateOr oc : tc.cases) {
				AreaName cn = oc.areaName();

				CaseChooser branch;
				CaseChooser dbranch;
				if (oc.cond == null) {
					branch = cc;
					dbranch = dcc;
				} else {
					String tfn = simpleName(oc.fnName);
					dbranch = dcc.handleCase(tfn);
					branch = cc.handleCase(tfn);
				}
				dbranch.code(cn);
				branch.code(cn);
				recurse(cx, cn, oc.template, areaName);
				if (oc.cond != null)
					callOnAssign(fn, oc.cond, area, sn, false, null);
			}
		} else if (tl instanceof RWD3Thing) {
			RWD3Thing d3 = (RWD3Thing) tl;
			callOnAssign(fn, d3.data, area, "D3Area.prototype._onUpdate", false, null);
		} else {
			throw new UtilException("Template of type " + tl.getClass() + " not supported");
		}
		if (tl instanceof RWTemplateFormat) {
			handleFormatsAndEvents(cx, area, areaName, fn, isEditable, (RWTemplateFormat)tl);
		}
		if (newVar != null) {
			cx.removeLastCopyVar();
		}
		area.done();
		return ret;
	}

	protected void handleFormatsAndEvents(GeneratorContext cx, AreaGenerator area, AreaName areaName, JSForm fn, boolean isEditable, RWTemplateFormat tl) {
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
					expr = cx.nil;
				expr = new ApplyExpr(((Locatable)o).location(), cx.cons, o, expr);
			} else
				throw new UtilException("Cannot handle format of type " + o.getClass());
		}
		if (expr != null) {
			if (simple.length() > 0)
				expr = new ApplyExpr(first, cx.cons, new StringLiteral(first, simple.substring(1)), expr);
			String scf = areaName.jsName() + ".prototype._setVariableFormats";
			JSForm scvs = JSForm.flex(scf + " = function()").needBlock();
			String tfn = tl.dynamicFunction.name;
			scvs.add(JSForm.flex("this._mydiv.setAttribute('class', join(FLEval.full(this."+tfn+"()), ' '))"));
			cx.target.add(scvs);
			area.setVarFormats(tfn);
			callOnAssign(fn, expr, area, scf, true, null);
		}
		else if (expr == null && simple.length() > 0) {
			fn.add(JSForm.flex("this._mydiv.className = '" + simple.substring(1) + "'"));
			area.setSimpleClass(simple.substring(1));
		}
		if (tl instanceof RWTemplateFormatEvents) {
			RWTemplateFormatEvents tfe = (RWTemplateFormatEvents) tl;
			if (!tfe.handlers.isEmpty()) {
				JSForm ahf = JSForm.flex(areaName.jsName() +".prototype._add_handlers = function()").needBlock();
				area.needAddHandlers();
				cx.target.add(ahf);
				boolean isFirst = true;
				for (RWEventHandler eh : tfe.handlers) {
					String tfn = eh.handlerFn.name;

					// add a hack to allow us to NOT overwrite events that we want to intercept first
					String distinguish = "";
					if (eh.action.equals("drop"))
						distinguish = "_";
					JSForm cev = JSForm.flex("this._mydiv['on" + distinguish + eh.action + "'] = function(event)").needBlock();
					cev.add(JSForm.flex("this._area._wrapper.dispatchEvent(this._area." + tfn + "(), event)"));
					ahf.add(cev);
	
					callOnAssign(fn, eh.expr, area, areaName.jsName() + ".prototype._add_handlers", isFirst, null);
					isFirst = false;
				}
			}
		}
	}
	
	private void createRules(GeneratorContext cx, RWContentExpr ce, AreaName areaName, Object container, String field) {
		JSForm rules = JSForm.flex(areaName.jsName() + "._rules =").needBlock();
		JSForm save = JSForm.flex("save: function(wrapper, text)").needBlock();
		if (ce.editFn != null) {
			save.add(JSForm.flex("var containingObject = " + ce.editFn + "()"));
		} else
			save.add(JSForm.flex("var containingObject = this._card"));
		// TODO: we may need to convert the text field to a more complex object type (e.g. integer) as specified in the rules we are given
		save.add(JSForm.flex("containingObject." + field + " = text"));
		// TODO: we need to consider which of the four types of change was just made (based on something put on atn)
		// 1. Transient local state (do nothing more)
		// 2. Persistent local state (save state object)
		// 3. Main object field or 4. Loaded object field (save data object using the appropriate contract)
		save.add(JSForm.flex("wrapper.saveObject(containingObject)"));
//		save.add(JSForm.flex("console.log('saved to:', containingObject)"));
		rules.add(save);
		// if we add another block, need "save.comma();"
		cx.target.add(rules);
	}

	protected void callOnAssign(JSForm addToFunc, Object valExpr, AreaGenerator area, String call, boolean addAssign, String moreArgs) {
		if (valExpr instanceof CardMember) {
			addToFunc.add(JSForm.flex("this._onAssign(this._card, '" + ((CardMember)valExpr).var + "', " + call + ")"));
			area.onAssign((CardMember)valExpr, call);
		} else if (valExpr instanceof TemplateListVar) {
			String var = ((TemplateListVar)valExpr).simpleName;
			addToFunc.add(JSForm.flex("this._src_" + var + "._interested(this, " + call + ")"));
			area.interested(var, call);
		} else if (valExpr instanceof CardFunction) {
			// we need to track down the function (if it's not in the object already) and callOnAssign it's definition
			CardFunction cf = (CardFunction) valExpr;
			String fullName = cf.clzName + "." + cf.function;
			RWFunctionDefinition fd = rewriter.functions.get(fullName);
			if (fd != null)
				for (RWFunctionCaseDefn fcd : fd.cases)
					callOnAssign(addToFunc, fcd.expr, area, call, false, moreArgs);
		} else if (valExpr instanceof LocalVar || valExpr instanceof StringLiteral || valExpr instanceof NumericLiteral || valExpr instanceof PackageVar || valExpr instanceof RWStructDefn) {
			// nothing to do here, not variable
		} else if (valExpr instanceof ApplyExpr) {
			ApplyExpr ae = (ApplyExpr) valExpr;
			if (ae.fn instanceof PackageVar && ((PackageVar)ae.fn).id.equals("FLEval.field")) {
				Object expr = ae.args.get(0);
				Expr dge = null;
				if (expr instanceof TemplateListVar) {
					callOnAssign(addToFunc, expr, area, call, false, moreArgs);
					String name = ((TemplateListVar)expr).simpleName;
					expr = "this._src_" + name + "." + name;
					if (area != null)
						dge = area.sourceFor(name);
				} else if (expr instanceof CardMember) {
					// need to handle if the whole member gets assigned
					callOnAssign(addToFunc, expr, area, call, false, moreArgs);
					// also handle if this field gets assigned
					if (area != null)
						dge = area.cardField((CardMember)expr);
					expr = "this._card." + ((CardMember)expr).var;
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
				String field = ((StringLiteral)ae.args.get(1)).text;
				addToFunc.add(JSForm.flex("this._onAssign(" + expr +", '" + field + "', " + call + ")"));
				area.onAssign(dge, field, call);
			} else {
				callOnAssign(addToFunc, ae.fn, area, call, false, moreArgs);
				for (Object o : ae.args)
					callOnAssign(addToFunc, o, area, call, false, moreArgs);
			}
		} else if (valExpr instanceof IfExpr) {
			IfExpr ie = (IfExpr) valExpr;
			callOnAssign(addToFunc, ie.guard, area, call, false, moreArgs);
			callOnAssign(addToFunc, ie.ifExpr, area, call, false, moreArgs);
			callOnAssign(addToFunc, ie.elseExpr, area, call, false, moreArgs);
		} else
			throw new UtilException("Not handled: " + valExpr.getClass());
		if (addAssign) {
			addToFunc.add(JSForm.flex(call + ".call(this" + (moreArgs != null ? ", " + moreArgs : "") + ")"));
			area.addAssign(call);
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
