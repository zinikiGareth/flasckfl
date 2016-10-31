package org.flasck.flas.template;

import java.util.ArrayList;
import java.util.List;

import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.commonBase.ApplyExpr;
import org.flasck.flas.commonBase.Locatable;
import org.flasck.flas.commonBase.StringLiteral;
import org.flasck.flas.commonBase.template.TemplateCases;
import org.flasck.flas.commonBase.template.TemplateExplicitAttr;
import org.flasck.flas.commonBase.template.TemplateFormat;
import org.flasck.flas.commonBase.template.TemplateLine;
import org.flasck.flas.commonBase.template.TemplateList;
import org.flasck.flas.commonBase.template.TemplateListVar;
import org.flasck.flas.commonBase.template.TemplateOr;
import org.flasck.flas.droidgen.CGRContext;
import org.flasck.flas.droidgen.DroidGenerator;
import org.flasck.flas.hsie.ApplyCurry;
import org.flasck.flas.hsie.HSIE;
import org.flasck.flas.jsform.JSForm;
import org.flasck.flas.jsform.JSTarget;
import org.flasck.flas.jsgen.Generator;
import org.flasck.flas.parsedForm.TemplateCardReference;
import org.flasck.flas.rewriter.Rewriter;
import org.flasck.flas.rewrittenForm.CardFunction;
import org.flasck.flas.rewrittenForm.CardMember;
import org.flasck.flas.rewrittenForm.LocalVar;
import org.flasck.flas.rewrittenForm.PackageVar;
import org.flasck.flas.rewrittenForm.RWContentExpr;
import org.flasck.flas.rewrittenForm.RWContentString;
import org.flasck.flas.rewrittenForm.RWD3Invoke;
import org.flasck.flas.rewrittenForm.RWEventHandler;
import org.flasck.flas.rewrittenForm.RWFunctionCaseDefn;
import org.flasck.flas.rewrittenForm.RWFunctionDefinition;
import org.flasck.flas.rewrittenForm.RWStructDefn;
import org.flasck.flas.rewrittenForm.RWTemplate;
import org.flasck.flas.rewrittenForm.RWTemplateCardReference;
import org.flasck.flas.rewrittenForm.RWTemplateDiv;
import org.flasck.flas.rewrittenForm.RWTemplateFormatEvents;
import org.flasck.flas.tokenizers.TemplateToken;
import org.flasck.flas.typechecker.TypeChecker;
import org.flasck.flas.vcode.hsieForm.HSIEForm;
import org.flasck.flas.vcode.hsieForm.HSIEForm.CodeType;
import org.zinutils.bytecode.Expr;
import org.zinutils.collections.CollectionUtils;
import org.zinutils.exceptions.UtilException;

public class TemplateGenerator {
	public class DefinedVar {
		final String name;
		final String definedIn;
		
		public DefinedVar(String name, String definedIn) {
			this.name = name;
			this.definedIn = definedIn;
		}
	}

	public class GeneratorContext {

		private final JSTarget target;
		private final String simpleName;
		private final String protoName;
		private int areaNo = 1;
		private String introduceVarHere;
		private final List<DefinedVar> varsToCopy = new ArrayList<DefinedVar>();
		private final Object nil;
		private final Object cons;
		private final Object equals;
		private final String javaName;

		public GeneratorContext(JSTarget target, Rewriter rw, RWTemplate cg) {
			this.target = target;
			this.javaName = cg.prefix;
			InputPosition posn = new InputPosition("template", 1, 1, "");
			this.simpleName = Generator.lname(cg.prefix, false);
			this.protoName = Generator.lname(cg.prefix, true);
			this.nil = rw.getMe(posn, "Nil");
			this.cons = rw.getMe(posn, "Cons");
			this.equals = rw.getMe(posn, "==");
		}
		
		String nextArea() {
			return this.simpleName + ".B" + (areaNo++);
		}

		public String currentVar() {
			return "b" + areaNo;
		}
		
		public void varToCopy(String s, String inClz) {
			varsToCopy.add(new DefinedVar(s, inClz));
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
	private final HSIE hsie;
	private final TypeChecker tc;
	private final ApplyCurry curry;
	private final DroidGenerator dg;

	public TemplateGenerator(Rewriter rewriter, HSIE hsie, TypeChecker tc, ApplyCurry curry, DroidGenerator dg) {
		this.rewriter = rewriter;
		this.hsie = hsie;
		this.tc = tc;
		this.curry = curry;
		this.dg = dg;
	}

	public void generate(Rewriter rw, JSTarget target) {
		for (RWTemplate cg : rewriter.templates)
			generateTemplate(rw, target, cg);
	}

	private void generateTemplate(Rewriter rw, JSTarget target, RWTemplate cg) {
		GeneratorContext cx = new GeneratorContext(target, rw, cg);
		JSForm ir = JSForm.flexFn(cx.protoName + "_render", CollectionUtils.listOf("doc", "wrapper", "parent"));
		target.add(ir);
		if (cg == null || cg.content == null)
			return;
		String topBlock = cx.nextArea();
		ir.add(JSForm.flex("new " + topBlock + "(new CardArea(parent, wrapper, this))"));
		
		dg.generateRender(cx.javaName, javaName(topBlock));
		
		recurse(cx, topBlock, cg.content, null);
	}

	private JSForm recurse(GeneratorContext cx, String called, TemplateLine tl, String parentClass) {
		if (tl == null)
			return null;
		JSForm fn = JSForm.flex(called +" = function(parent)").needBlock();
		cx.target.add(fn);
		String base;
		String moreArgs = "";
		boolean isEditable = false;
		String customTag = null;
		if (tl instanceof RWTemplateDiv) {
			RWTemplateDiv td = (RWTemplateDiv) tl;
			base = "DivArea";
			if (td.customTag != null) {
				customTag = td.customTag;
				moreArgs = ", '" + td.customTag + "'";
				if (td.customTag.equals("svg"))
					moreArgs = moreArgs + ", 'http://www.w3.org/2000/svg'";
			}
			// TODO: a variable custom tag is hard & needs "assign" logic
		} else if (tl instanceof TemplateList) {
			TemplateList ul = (TemplateList) tl;
			base = "ListArea";
			if (ul.customTag != null) {
				moreArgs = ", '" + ul.customTag + "'";
				if (ul.customTag.equals("svg"))
					moreArgs = moreArgs + ", 'http://www.w3.org/2000/svg'";
			}
			// TODO: a variable custom tag is hard & needs "assign" logic
		} else if (tl instanceof RWContentString || tl instanceof RWContentExpr) {
			base = "TextArea";
			isEditable = tl instanceof RWContentExpr && ((RWContentExpr)tl).editable();
		} else if (tl instanceof RWTemplateCardReference) {
			RWTemplateCardReference cr = (RWTemplateCardReference) tl;
			base = "CardSlotArea";
			if (cr.explicitCard != null)
				moreArgs = ", { explicit: " + cr.explicitCard + "}";
			else if (cr.yoyoVar != null) {
				moreArgs = ", undefined"; // explicitly say the card is undefined until yoyoVar evaluates
			} else
				throw new UtilException("Can't handle this case");
		} else if (tl instanceof TemplateCases) {
			base = "CasesArea";
		} else if (tl instanceof RWD3Invoke) {
			base = "D3Area";
		} else {
			throw new UtilException("Template of type " + (tl == null ? "null":tl.getClass()) + " not supported");
		}
		CGRContext cgrx = dg.area(javaName(called), base, customTag);
		fn.add(JSForm.flex(base +".call(this, parent" + moreArgs + ")"));
		fn.add(JSForm.flex("if (!parent) return"));
		for (DefinedVar vc : cx.varsToCopy) {
			String s = vc.name;
			fn.add(JSForm.flex("this._src_" + s + " = parent._src_" + s));
			dg.copyVar(cgrx, javaName(parentClass), javaName(vc.definedIn), s);
		}
		String newVar = cx.extractNewVar();
		if (newVar != null) {
			fn.add(JSForm.flex("this._src_"+newVar+ " = this"));
			cx.varToCopy(newVar, called);
			dg.newVar(cgrx, newVar);
		}
		cx.target.add(JSForm.flex(called +".prototype = new " + base + "()"));
		cx.target.add(JSForm.flex(called +".prototype.constructor = " + called));
		if (newVar != null) {
			JSForm nda = JSForm.flex(called +".prototype._assignToVar = function(obj)").needBlock();
			nda.add(JSForm.flex("if (this. " + newVar + " == obj) return"));
			JSForm ifremove = JSForm.flex("if (this." + newVar+ ")");
			// TODO: I claim this should be this.newVar, not obj
			ifremove.add(JSForm.flex(" this._wrapper.removeOnUpdate('crorepl', this._parent._croset, obj.id, this)"));
			nda.add(ifremove);
			nda.add(JSForm.flex("this." + newVar + " = obj"));
			JSForm ifload = JSForm.flex("if (this." + newVar+ ")").needBlock();
			// TODO: I claim this should also be this.newVar, not obj for consistency, but at least they are the same here ...
			ifload.add(JSForm.flex("this._wrapper.onUpdate('crorepl', this._parent._croset, obj.id, this)"));
			nda.add(ifload);
			nda.add(JSForm.flex("this._fireInterests()"));
			cx.target.add(nda);
			dg.assignToVar(cgrx, newVar);
		}
		if (tl instanceof RWTemplateDiv) {
			RWTemplateDiv td = (RWTemplateDiv) tl;
			int an = 1;
			for (Object a : td.attrs) {
				if (a instanceof TemplateExplicitAttr) {
					TemplateExplicitAttr tea = (TemplateExplicitAttr) a;
					switch (tea.type) {
					case TemplateToken.STRING: {
						fn.add(JSForm.flex("this._mydiv.setAttribute('" + tea.attr + "', '" + tea.value +"')"));
						break;
					}
					case TemplateToken.IDENTIFIER: {
						String saf = called + ".prototype._setAttr_" + an;
						JSForm sak = JSForm.flex(saf + " = function()").needBlock();
						// pull from teas
						HSIEForm form = hsie.handleExpr(tea.value, CodeType.AREA);
						JSForm.assign(sak, "var attr", form);
						sak.add(JSForm.flex("attr = FLEval.full(attr)"));
						JSForm ifassign = JSForm.flex("if (attr && !(attr instanceof FLError))").needBlock();
						sak.add(ifassign);
//						ifassign.add(JSForm.flex("console.log('setting attribute " + tea.attr +" on', this._mydiv.id, 'to', attr)"));
						ifassign.add(JSForm.flex("this._mydiv.setAttribute('" + tea.attr +"', attr)"));
						cx.target.add(sak);
//						fn.add(JSForm.flex("this._setAttr_" + an +"()"));
						callOnAssign(fn, tea.value, cgrx, saf, true, null);
						an++;
						break;
					}
					default:
						throw new UtilException("Cannot handle TEA type " + tea.type);
					}
				} else
					throw new UtilException("Cannot handle attr " + a.getClass());
			}
			if (td.droppables != null) {
				List<String> asRegexps = new ArrayList<String>();
				for (String s : td.droppables)
					asRegexps.add("/" + s + "/");
				fn.add(JSForm.flex("this._dropSomethingHere(" + asRegexps + ")"));
			}
			for (TemplateLine c : td.nested) {
				String v = cx.currentVar();
				String cn = cx.nextArea();
				fn.add(JSForm.flex("var " + v + " = new " + cn + "(this)"));
				dg.createNested(cgrx, v, javaName(cn));
				recurse(cx, cn, c, called);
			}
		} else if (tl instanceof TemplateList) {
			TemplateList l = (TemplateList) tl;
			TemplateListVar lv = (TemplateListVar)l.iterVar;
			String tlv = lv == null ? null : lv.name;
			if (l.supportDragOrdering)
				fn.add(JSForm.flex("this._supportDragging()"));
			String item = cx.nextArea();
			{
				JSForm nc = JSForm.flex(called +".prototype._newChild = function()").needBlock();
				nc.add(JSForm.flex("return new " + item + "(this)"));
				cx.target.add(nc);
			}
			dg.newListChild(cgrx, javaName(item));
			if (tlv != null)
				cx.newVar(tlv);
			JSForm cfn = recurse(cx, item, l.template, called);
			if (l.supportDragOrdering)
				cfn.add(JSForm.flex("this._makeDraggable()"));
			JSForm atv = JSForm.flex(called + ".prototype._assignToVar = function()").needBlock();
			// pull from lvs
			HSIEForm form = hsie.handleExpr(l.listVar, CodeType.AREA);
			JSForm.assign(atv, "var lv", form);
			atv.add(JSForm.flex("lv = FLEval.full(lv)"));
			callOnAssign(fn, l.listVar, cgrx, called + ".prototype._assignToVar", false, "lv");
			fn.add(JSForm.flex(called + ".prototype._assignToVar.call(this)"));
			atv.add(JSForm.flex("ListArea.prototype._assignToVar.call(this, lv)"));
			cx.target.add(atv);
		} else if (tl instanceof RWContentString) {
			RWContentString cs = (RWContentString) tl;
			fn.add(JSForm.flex("this._setText('" + cs.text + "')"));
			dg.setText(cgrx, cs.text);
		} else if (tl instanceof RWContentExpr) {
			RWContentExpr ce = (RWContentExpr)tl;
			Object valExpr = ce.expr;
			callOnAssign(fn, valExpr, cgrx, called + ".prototype._contentExpr", true, null);

			JSForm cexpr = JSForm.flex(called +".prototype._contentExpr = function()").needBlock();
			// pull from contents
			HSIEForm form = hsie.handleExpr(valExpr, CodeType.AREA);
			form.dump(TypeChecker.logger);
			JSForm.assign(cexpr, "var str", form);
			if (ce.rawHTML)
				cexpr.add(JSForm.flex("this._insertHTML(str)"));
			else
				cexpr.add(JSForm.flex("this._assignToText(str)"));
			cx.target.add(cexpr);

			dg.contentExpr(cgrx, form, ce.rawHTML);
			
			if (isEditable) {
				// for it to be editable, it must be a clear field of a clear object
				if (valExpr instanceof CardMember) {
					CardMember cm = (CardMember) valExpr;
					fn.add(JSForm.flex("this._editable(" + called + "._rules)"));
					createRules(cx, called, null, cm.var);
				} else if (valExpr instanceof ApplyExpr) {
					ApplyExpr ae = (ApplyExpr) valExpr;
					if (!(ae.fn instanceof PackageVar) || !((PackageVar)ae.fn).uniqueName().equals("FLEval.field"))
						throw new UtilException("Cannot edit: " + ae);
					fn.add(JSForm.flex("this._editable(" + called + "._rules)"));
					createRules(cx, called, ae.args.get(0), ((StringLiteral)ae.args.get(1)).text);
				} else 
					throw new UtilException("Cannot edit: " + valExpr);
			}
		} else if (tl instanceof RWTemplateCardReference) {
			RWTemplateCardReference cr = (RWTemplateCardReference) tl;
			if (cr.explicitCard != null)
				; // fully handled above
			else if (cr.yoyoVar != null) {
				Object valExpr = cr.yoyoVar;
				callOnAssign(fn, valExpr, cgrx, called + ".prototype._yoyoExpr", true, null);
	
				JSForm cexpr = JSForm.flex(called +".prototype._yoyoExpr = function()").needBlock();
				HSIEForm form = hsie.handleExpr(valExpr, CodeType.AREA);
				form.dump(TypeChecker.logger);
				JSForm.assign(cexpr, "var card", form);
				cexpr.add(JSForm.flex("this._updateToCard(card)"));
				cx.target.add(cexpr);

				dg.yoyoExpr(cgrx, form);
			} else
				throw new UtilException("handle this case");
		} else if (tl instanceof TemplateCases) {
			TemplateCases tc = (TemplateCases) tl;
			String sn = called + ".prototype._chooseCase";
			JSForm sw = JSForm.flex(sn +" = function(parent)").needBlock();
			sw.add(JSForm.flex("\"use strict\""));
			sw.add(JSForm.flex("var cond"));
			cx.target.add(sw);
			callOnAssign(fn, tc.switchOn, cgrx, sn, true, null);

			for (TemplateOr oc : tc.cases) {
				String cn = cx.nextArea();

				JSForm doit;
				if (oc.cond == null)
					doit = sw;
				else {
					JSForm.assign(sw, "cond", hsie.handleExpr(new ApplyExpr(oc.location(), cx.equals, tc.switchOn, oc.cond), CodeType.AREA));
					doit = JSForm.flex("if (FLEval.full(cond))").needBlock();
					sw.add(doit);
				}
				doit.add(JSForm.flex("this._setTo(" + cn +")"));
//				doit.add(JSForm.flex("var v = new " + cn + "(this)"));
				doit.add(JSForm.flex("return"));
				recurse(cx, cn, oc.template, called);
				if (oc.cond != null)
					callOnAssign(fn, oc.cond, null, sn, false, null);
			}
		} else if (tl instanceof RWD3Invoke) {
			RWD3Invoke d3 = (RWD3Invoke) tl;
			callOnAssign(fn, d3.d3.data, null, "D3Area.prototype._onUpdate", false, null);
		} else {
			throw new UtilException("Template of type " + tl.getClass() + " not supported");
		}
		if (tl instanceof TemplateFormat) {
			handleFormatsAndEvents(cx, cgrx, called, fn, isEditable, (TemplateFormat)tl);
		}
		if (newVar != null) {
			cx.removeLastCopyVar();
		}
		dg.done(cgrx);
		return fn;
	}

	protected void handleFormatsAndEvents(GeneratorContext cx, CGRContext cgrx, String called, JSForm fn, boolean isEditable, TemplateFormat tl) {
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
			String scf = called + ".prototype._setVariableFormats";
			JSForm scvs = JSForm.flex(scf + " = function()").needBlock();
			// pull from formats
			HSIEForm form = hsie.handleExpr(expr, CodeType.AREA);
			JSForm.assign(scvs, "var attr", form);
			scvs.add(JSForm.flex("attr = FLEval.full(attr)"));
			scvs.add(JSForm.flex("this._mydiv.setAttribute('class', join(FLEval.full(attr), ' '))"));
			cx.target.add(scvs);
			dg.setVarFormats(cgrx, form);
			callOnAssign(fn, expr, cgrx, scf, true, null);
		}
		else if (expr == null && simple.length() > 0) {
			fn.add(JSForm.flex("this._mydiv.className = '" + simple.substring(1) + "'"));
			dg.setSimpleClass(cgrx, simple.substring(1));
		}
		if (tl instanceof RWTemplateFormatEvents) {
			RWTemplateFormatEvents tfe = (RWTemplateFormatEvents) tl;
			if (!tfe.handlers.isEmpty()) {
				JSForm ahf = JSForm.flex(called +".prototype._add_handlers = function()").needBlock();
				dg.needAddHandlers(cgrx);
				cx.target.add(ahf);
				boolean isFirst = true;
				for (RWEventHandler eh : tfe.handlers) {
					// pull from handlers
					HSIEForm exprn = hsie.handleExpr(eh.expr, HSIEForm.CodeType.AREA);
					curry.rewrite(tc, exprn);

					// add a hack to allow us to NOT overwrite events that we want to intercept first
					String distinguish = "";
					if (eh.action.equals("drop"))
						distinguish = "_";
					JSForm.assign(ahf, "var eh" + eh.action, exprn);
					JSForm cev = JSForm.flex("this._mydiv['on" + distinguish + eh.action + "'] = function(event)").needBlock();
					cev.add(JSForm.flex("this._area._wrapper.dispatchEvent(eh" + eh.action + ", event)"));
					ahf.add(cev);
	
					callOnAssign(fn, eh.expr, cgrx, called + ".prototype._add_handlers", isFirst, null);
					isFirst = false;
				}
			}
		}
	}
	
	private void createRules(GeneratorContext cx, String area, Object container, String field) {
		JSForm rules = JSForm.flex(area + "._rules =").needBlock();
		JSForm save = JSForm.flex("save: function(wrapper, text)").needBlock();
		if (container != null) {
			JSForm.assign(save, "var containingObject", hsie.handleExpr(container, CodeType.AREA));
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

	protected void callOnAssign(JSForm addToFunc, Object valExpr, CGRContext cgrx, String call, boolean addAssign, String moreArgs) {
		if (valExpr instanceof CardMember) {
			addToFunc.add(JSForm.flex("this._onAssign(this._card, '" + ((CardMember)valExpr).var + "', " + call + ")"));
			dg.onAssign(cgrx, (CardMember)valExpr, call);
		} else if (valExpr instanceof TemplateListVar) {
			String var = ((TemplateListVar)valExpr).name;
			addToFunc.add(JSForm.flex("this._src_" + var + "._interested(this, " + call + ")"));
			dg.interested(cgrx, var, call);
		} else if (valExpr instanceof CardFunction) {
			// we need to track down the function (if it's not in the object already) and callOnAssign it's definition
			CardFunction cf = (CardFunction) valExpr;
			String fullName = cf.clzName + "." + cf.function;
			RWFunctionDefinition fd = rewriter.functions.get(fullName);
			if (fd != null)
				for (RWFunctionCaseDefn fcd : fd.cases)
					callOnAssign(addToFunc, fcd.expr, cgrx, call, false, moreArgs);
		} else if (valExpr instanceof LocalVar || valExpr instanceof StringLiteral || valExpr instanceof PackageVar || valExpr instanceof RWStructDefn) {
			// nothing to do here, not variable
		} else if (valExpr instanceof ApplyExpr) {
			ApplyExpr ae = (ApplyExpr) valExpr;
			if (ae.fn instanceof PackageVar && ((PackageVar)ae.fn).id.equals("FLEval.field")) {
				Object expr = ae.args.get(0);
				Expr dge = null;
				if (expr instanceof TemplateListVar) {
					callOnAssign(addToFunc, expr, cgrx, call, false, moreArgs);
					String name = ((TemplateListVar)expr).name;
					expr = "this._src_" + name + "." + name;
					if (cgrx != null)
						dge = cgrx.ctor.getField(cgrx.ctor.getField(cgrx.ctor.myThis(), "_src_" + name), name);
				} else if (expr instanceof CardMember) {
					// need to handle if the whole member gets assigned
					callOnAssign(addToFunc, expr, cgrx, call, false, moreArgs);
					// also handle if this field gets assigned
					if (cgrx != null)
						dge = cgrx.ctor.getField(cgrx.ctor.getField(cgrx.ctor.myThis(), "_card"), ((CardMember)expr).var);
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
				dg.onAssign(cgrx, dge, field, call);
			} else {
				callOnAssign(addToFunc, ae.fn, cgrx, call, false, moreArgs);
				for (Object o : ae.args)
					callOnAssign(addToFunc, o, cgrx, call, false, moreArgs);
			}
		} else
			throw new UtilException("Not handled: " + valExpr.getClass());
		if (addAssign) {
			addToFunc.add(JSForm.flex(call + ".call(this" + (moreArgs != null ? ", " + moreArgs : "") + ")"));
			dg.addAssign(cgrx, call);
		}
	}

	private String javaName(String topBlock) {
		int idx = topBlock.lastIndexOf("._");
		return topBlock.substring(0, idx+1) + topBlock.substring(idx+2);
	}
}
