package org.flasck.flas.template;

import java.util.ArrayList;
import java.util.List;

import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.hsie.ApplyCurry;
import org.flasck.flas.hsie.HSIE;
import org.flasck.flas.jsform.JSForm;
import org.flasck.flas.jsform.JSTarget;
import org.flasck.flas.jsgen.Generator;
import org.flasck.flas.parsedForm.AbsoluteVar;
import org.flasck.flas.parsedForm.ApplyExpr;
import org.flasck.flas.parsedForm.CardFunction;
import org.flasck.flas.parsedForm.CardMember;
import org.flasck.flas.parsedForm.CardReference;
import org.flasck.flas.parsedForm.ContentExpr;
import org.flasck.flas.parsedForm.ContentString;
import org.flasck.flas.parsedForm.D3Invoke;
import org.flasck.flas.parsedForm.EventHandler;
import org.flasck.flas.parsedForm.FunctionCaseDefn;
import org.flasck.flas.parsedForm.FunctionDefinition;
import org.flasck.flas.parsedForm.LocalVar;
import org.flasck.flas.parsedForm.StringLiteral;
import org.flasck.flas.parsedForm.Template;
import org.flasck.flas.parsedForm.TemplateCases;
import org.flasck.flas.parsedForm.TemplateDiv;
import org.flasck.flas.parsedForm.TemplateExplicitAttr;
import org.flasck.flas.parsedForm.TemplateFormat;
import org.flasck.flas.parsedForm.TemplateLine;
import org.flasck.flas.parsedForm.TemplateList;
import org.flasck.flas.parsedForm.TemplateListVar;
import org.flasck.flas.parsedForm.TemplateOr;
import org.flasck.flas.rewriter.Rewriter;
import org.flasck.flas.tokenizers.TemplateToken;
import org.flasck.flas.typechecker.TypeChecker;
import org.flasck.flas.vcode.hsieForm.HSIEForm;
import org.flasck.flas.vcode.hsieForm.HSIEForm.CodeType;
import org.zinutils.collections.CollectionUtils;
import org.zinutils.exceptions.UtilException;

public class TemplateGenerator {
	public class GeneratorContext {

		private final JSTarget target;
		private final String simpleName;
		private final String protoName;
		private int areaNo = 1;
		private String introduceVarHere;
		private final List<String> varsToCopy = new ArrayList<String>();
		private final AbsoluteVar nil;
		private final AbsoluteVar cons;
		private final AbsoluteVar equals;

		public GeneratorContext(JSTarget target, Template cg) {
			this.target = target;
			this.simpleName = Generator.lname(cg.prefix, false);
			this.protoName = Generator.lname(cg.prefix, true);
			InputPosition gen = new InputPosition("generator", 0, 0, null);
			this.nil = cg.scope.fromRoot(gen, "Nil");
			this.cons = cg.scope.fromRoot(gen, "Cons");
			this.equals = cg.scope.fromRoot(gen, "==");
		}
		
		String nextArea() {
			return this.simpleName + ".B" + (areaNo++);
		}

		public String currentVar() {
			return "b" + areaNo;
		}
		
		public void varToCopy(String s) {
			varsToCopy.add(s);
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

	public TemplateGenerator(Rewriter rewriter, HSIE hsie, TypeChecker tc, ApplyCurry curry) {
		this.rewriter = rewriter;
		this.hsie = hsie;
		this.tc = tc;
		this.curry = curry;
	}

	public void generate(JSTarget target) {
		for (Template cg : rewriter.templates)
			generateTemplate(target, cg);
	}

	private void generateTemplate(JSTarget target, Template cg) {
		GeneratorContext cx = new GeneratorContext(target, cg);
		JSForm ir = JSForm.flexFn(cx.protoName + "_render", CollectionUtils.listOf("doc", "wrapper", "parent"));
		String topBlock = cx.nextArea();
		ir.add(JSForm.flex("new " + topBlock + "(new CardArea(parent, wrapper, this))"));
		target.add(ir);
		
		recurse(cx, topBlock, cg.content);
	}

	private JSForm recurse(GeneratorContext cx, String called, TemplateLine tl) {
		JSForm fn = JSForm.flex(called +" = function(parent)").needBlock();
		cx.target.add(fn);
		String base;
		String moreArgs = "";
		boolean isEditable = false;
		if (tl instanceof TemplateDiv) {
			TemplateDiv td = (TemplateDiv) tl;
			base = "DivArea";
			if (td.customTag != null) {
				moreArgs = ", '" + td.customTag + "'";
				if (td.customTag.equals("svg"))
					moreArgs = moreArgs + ", 'http://www.w3.org/2000/svg'";
			}
			// TODO: a variable custom tag is hard & needs "assign" logic
		} else if (tl instanceof TemplateList) {
			base = "ListArea";
		} else if (tl instanceof ContentString || tl instanceof ContentExpr) {
			base = "TextArea";
			isEditable = tl instanceof ContentExpr && ((ContentExpr)tl).editable();
		} else if (tl instanceof CardReference) {
			CardReference cr = (CardReference) tl;
			base = "CardSlotArea";
			moreArgs = ", { card: " + cr.explicitCard + "}";
		} else if (tl instanceof TemplateCases) {
			base = "CasesArea";
		} else if (tl instanceof D3Invoke) {
			base = "D3Area";
		} else {
			throw new UtilException("Template of type " + tl.getClass() + " not supported");
		}
		fn.add(JSForm.flex(base +".call(this, parent" + moreArgs + ")"));
		fn.add(JSForm.flex("if (!parent) return"));
		for (String s : cx.varsToCopy)
			fn.add(JSForm.flex("this._src_" + s + " = parent._src_" + s));
		String newVar = cx.extractNewVar();
		if (newVar != null) {
			fn.add(JSForm.flex("this._src_"+newVar+ " = this"));
			cx.varToCopy(newVar);
		}
		cx.target.add(JSForm.flex(called +".prototype = new " + base + "()"));
		cx.target.add(JSForm.flex(called +".prototype.constructor = " + called));
		if (newVar != null) {
			JSForm nda = JSForm.flex(called +".prototype._assignToVar = function(obj)").needBlock();
			nda.add(JSForm.flex("if (this. " + newVar + " == obj) return"));
			JSForm ifremove = JSForm.flex("if (this." + newVar+ ")");
			ifremove.add(JSForm.flex(" this._wrapper.removeOnUpdate('crorepl', this._parent._croset, obj.id, this)"));
			nda.add(ifremove);
			nda.add(JSForm.flex("this." + newVar + " = obj"));
			JSForm ifload = JSForm.flex("if (this." + newVar+ ")").needBlock();
			ifload.add(JSForm.flex("this._wrapper.onUpdate('crorepl', this._parent._croset, obj.id, this)"));
			nda.add(ifload);
			nda.add(JSForm.flex("this._fireInterests()"));
			cx.target.add(nda);
		}
		if (tl instanceof TemplateDiv) {
			TemplateDiv td = (TemplateDiv) tl;
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
						HSIEForm form = hsie.handleExpr(tea.value, CodeType.AREA);
						JSForm.assign(sak, "var attr", form);
						sak.add(JSForm.flex("attr = FLEval.full(attr)"));
						JSForm ifassign = JSForm.flex("if (attr && !(attr instanceof FLError))").needBlock();
						sak.add(ifassign);
//						ifassign.add(JSForm.flex("console.log('setting attribute " + tea.attr +" on', this._mydiv.id, 'to', attr)"));
						ifassign.add(JSForm.flex("this._mydiv.setAttribute('" + tea.attr +"', attr)"));
						cx.target.add(sak);
//						fn.add(JSForm.flex("this._setAttr_" + an +"()"));
						callOnAssign(fn, tea.value, saf, true);
						an++;
						break;
					}
					default:
						throw new UtilException("Cannot handle TEA type " + tea.type);
					}
				} else
					throw new UtilException("Cannot handle attr " + a.getClass());
			}
			if (!td.handlers.isEmpty()) {
				JSForm ahf = JSForm.flex(called +".prototype._add_handlers = function()").needBlock();
				cx.target.add(ahf);
				boolean first = true;
				for (EventHandler eh : td.handlers) {
					HSIEForm expr = hsie.handleExpr(eh.expr, HSIEForm.CodeType.AREA);
					curry.rewrite(tc, expr);
					
					JSForm.assign(ahf, "var eh" + eh.action, expr);
					JSForm cev = JSForm.flex("this._mydiv['on" + eh.action + "'] = function(event)").needBlock();
					cev.add(JSForm.flex("this._area._wrapper.dispatchEvent(eh" + eh.action + ", event)"));
					ahf.add(cev);
	
					callOnAssign(fn, eh.expr, called + ".prototype._add_handlers", first);
					first = false;
				}
			}
			for (TemplateLine c : td.nested) {
				String v = cx.currentVar();
				String cn = cx.nextArea();
				fn.add(JSForm.flex("var " + v + " = new " + cn + "(this)"));
				recurse(cx, cn, c);
				if (c instanceof TemplateList) {
					TemplateList l = (TemplateList) c;
					CardMember ce = (CardMember) l.listVar; // may need to consider alternatives
					fn.add(JSForm.flex(v + "._assignToVar(this._card."+ce.var+")"));
				}
			}
		} else if (tl instanceof TemplateList) {
			TemplateList l = (TemplateList) tl;
			String tlv = ((TemplateListVar)l.iterVar).name;
			if (l.supportDragOrdering)
				fn.add(JSForm.flex("this._supportDragging()"));
			JSForm nc = JSForm.flex(called +".prototype._newChild = function()").needBlock();
			String item = cx.nextArea();
			nc.add(JSForm.flex("return new " + item + "(this)"));
			cx.target.add(nc);
			cx.newVar(tlv);
			JSForm cfn = recurse(cx, item, l.template);
			if (l.supportDragOrdering)
				cfn.add(JSForm.flex("this._makeDraggable()"));
		} else if (tl instanceof ContentString) {
			ContentString cs = (ContentString) tl;
			fn.add(JSForm.flex("this._setText('" + cs.text + "')"));
		} else if (tl instanceof ContentExpr) {
			ContentExpr ce = (ContentExpr)tl;
			Object valExpr = ce.expr;
			callOnAssign(fn, valExpr, called + ".prototype._contentExpr", true);

			JSForm cexpr = JSForm.flex(called +".prototype._contentExpr = function()").needBlock();
			HSIEForm form = hsie.handleExpr(valExpr, CodeType.AREA);
			form.dump(TypeChecker.logger);
			JSForm.assign(cexpr, "var str", form);
			cexpr.add(JSForm.flex("this._assignToText(str)"));
			cx.target.add(cexpr);

			if (isEditable) {
				// for it to be editable, it must be a clear field of a clear object
				if (valExpr instanceof CardMember) {
					CardMember cm = (CardMember) valExpr;
					fn.add(JSForm.flex("this._editable(" + called + "._rules)"));
					createRules(cx, called, null, cm.var);
				} else if (valExpr instanceof ApplyExpr) {
					ApplyExpr ae = (ApplyExpr) valExpr;
					if (!(ae.fn instanceof AbsoluteVar) || !((AbsoluteVar)ae.fn).uniqueName().equals("FLEval.field"))
						throw new UtilException("Cannot edit: " + ae);
					fn.add(JSForm.flex("this._editable(" + called + "._rules)"));
					createRules(cx, called, ae.args.get(0), ((StringLiteral)ae.args.get(1)).text);
				} else 
					throw new UtilException("Cannot edit: " + valExpr);
			}
		} else if (tl instanceof CardReference) {
			// Not sure if we need to do something or not ...
		} else if (tl instanceof TemplateCases) {
			TemplateCases tc = (TemplateCases) tl;
			String sn = called + ".prototype._chooseCase";
			JSForm sw = JSForm.flex(sn +" = function(parent)").needBlock();
			sw.add(JSForm.flex("\"use strict\""));
			sw.add(JSForm.flex("var cond"));
			cx.target.add(sw);
			callOnAssign(fn, tc.switchOn, sn, true);

			for (TemplateOr oc : tc.cases) {
				String cn = cx.nextArea();

				JSForm.assign(sw, "cond", hsie.handleExpr(new ApplyExpr(oc.location(), cx.equals, tc.switchOn, oc.cond), CodeType.AREA));
				JSForm doit = JSForm.flex("if (FLEval.full(cond))").needBlock();
				doit.add(JSForm.flex("this._setTo(" + cn +")"));
//				doit.add(JSForm.flex("var v = new " + cn + "(this)"));
				doit.add(JSForm.flex("return"));
				sw.add(doit);
				recurse(cx, cn, oc.template);
				callOnAssign(fn, oc.cond, sn, false);
			}
		} else if (tl instanceof D3Invoke) {
			D3Invoke d3 = (D3Invoke) tl;
			callOnAssign(fn, d3.d3.data, "D3Area.prototype._onUpdate", false);
		} else {
			throw new UtilException("Template of type " + tl.getClass() + " not supported");
		}
		if (tl instanceof TemplateFormat) {
			handleFormats(cx, called, fn, isEditable, ((TemplateFormat)tl).formats);
		}
		if (newVar != null) {
			cx.removeLastCopyVar();
		}
		return fn;
	}

	protected void handleFormats(GeneratorContext cx, String called, JSForm fn, boolean isEditable, List<Object> formats) {
		StringBuilder simple = new StringBuilder();
		if (isEditable)
			simple.append(" flasck-editable");
		Object expr = null;
		InputPosition first = null;
		for (Object o : formats) {
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
			} else if (o instanceof ApplyExpr) {
				// TODO: need to collect object/field pairs that we depend on
				ApplyExpr ae = (ApplyExpr) o;
				if (expr == null)
					expr = cx.nil;
				expr = new ApplyExpr(ae.location, cx.cons, o, expr);
			} else
				throw new UtilException("Cannot handle format of type " + o.getClass());
		}
		if (expr != null) {
			if (simple.length() > 0)
				expr = new ApplyExpr(first, cx.cons, new StringLiteral(first, simple.substring(1)), expr);
			String scf = called + ".prototype._setVariableFormats";
			JSForm scvs = JSForm.flex(scf + " = function()").needBlock();
			HSIEForm form = hsie.handleExpr(expr, CodeType.AREA);
			JSForm.assign(scvs, "var attr", form);
			scvs.add(JSForm.flex("attr = FLEval.full(attr)"));
			scvs.add(JSForm.flex("this._mydiv.setAttribute('class', join(FLEval.full(attr), ' '))"));
			cx.target.add(scvs);
			callOnAssign(fn, expr, scf, true);
		}
		else if (expr == null && simple.length() > 0) {
			fn.add(JSForm.flex("this._mydiv.className = '" + simple.substring(1) + "'"));
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
		save.add(JSForm.flex("console.log('saved to:', containingObject)"));
		rules.add(save);
		// if we add another block, need "save.comma();"
		cx.target.add(rules);
	}

	protected void callOnAssign(JSForm addToFunc, Object valExpr, String call, boolean addAssign) {
		if (valExpr instanceof CardMember) {
			addToFunc.add(JSForm.flex("this._onAssign(this._card, '" + ((CardMember)valExpr).var + "', " + call + ")"));
		} else if (valExpr instanceof TemplateListVar) {
			String var = ((TemplateListVar)valExpr).name;
			addToFunc.add(JSForm.flex("this._src_" + var + "._interested(this, " + call + ")"));
		} else if (valExpr instanceof CardFunction) {
			// we need to track down the function (if it's not in the object already) and callOnAssign it's definition
			CardFunction cf = (CardFunction) valExpr;
			String fullName = cf.clzName + "." + cf.function;
			FunctionDefinition fd = rewriter.functions.get(fullName);
			if (fd != null)
				for (FunctionCaseDefn fcd : fd.cases)
					callOnAssign(addToFunc, fcd.expr, call, false);
		} else if (valExpr instanceof LocalVar || valExpr instanceof StringLiteral || valExpr instanceof AbsoluteVar) {
			// nothing to do here, not variable
		} else if (valExpr instanceof ApplyExpr) {
			ApplyExpr ae = (ApplyExpr) valExpr;
			if (ae.fn instanceof AbsoluteVar && ((AbsoluteVar)ae.fn).id.equals("FLEval.field")) {
				Object expr = ae.args.get(0);
				if (expr instanceof TemplateListVar) {
					callOnAssign(addToFunc, expr, call, false);
					String name = ((TemplateListVar)expr).name;
					expr = "this._src_" + name + "." + name;
				} else if (expr instanceof CardMember) {
					// need to handle if the whole member gets assigned
					callOnAssign(addToFunc, expr, call, false);
					// also handle if this field gets assigned
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
			} else {
				callOnAssign(addToFunc, ae.fn, call, false);
				for (Object o : ae.args)
					callOnAssign(addToFunc, o, call, false);
			}
		} else
			throw new UtilException("Not handled: " + valExpr.getClass());
		if (addAssign)
			addToFunc.add(JSForm.flex(call + ".call(this)"));
	}
}
