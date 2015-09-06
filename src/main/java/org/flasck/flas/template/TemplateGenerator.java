package org.flasck.flas.template;

import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;

import org.flasck.flas.TemplateAbstractModel;
import org.flasck.flas.TemplateAbstractModel.AbstractTreeNode;
import org.flasck.flas.TemplateAbstractModel.Handler;
import org.flasck.flas.TemplateAbstractModel.OrCase;
import org.flasck.flas.TemplateAbstractModel.VisualTree;
import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.errors.ErrorResult;
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
		private final Object nil;
		private final Object cons;

		public GeneratorContext(JSTarget target, Template cg) {
			this.target = target;
			this.simpleName = Generator.lname(cg.prefix, false);
			this.protoName = Generator.lname(cg.prefix, true);
			this.nil = cg.scope.fromRoot(null, "Nil").defn;
			this.cons = cg.scope.fromRoot(null, "Cons").defn;
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

	private final ErrorResult errors;
	private final Rewriter rewriter;
	private final HSIE hsie;
	private final TypeChecker tc;
	private final ApplyCurry curry;

	public TemplateGenerator(ErrorResult errors, Rewriter rewriter, HSIE hsie, TypeChecker tc, ApplyCurry curry) {
		this.errors = errors;
		this.rewriter = rewriter;
		this.hsie = hsie;
		this.tc = tc;
		this.curry = curry;
	}

	public void generate(JSTarget target) {
		for (Template cg : rewriter.templates) {
			if (cg.prefix.startsWith("net.ziniki.perspocpoc")) {
				latestTemplateGeneration(target, cg);
				continue;
			}
			TemplateAbstractModel tam = makeAbstractTemplateModel(errors, rewriter, hsie, cg);
			generate(target, tam, null, null);
			JSForm onUpdate = JSForm.flex(tam.prefix + ".onUpdate =").needBlock();
			JSForm prev = null;
			for (String field : tam.fields.key1Set()) {
				if (prev != null)
					prev.comma();
				JSForm curr = JSForm.flex("'" + field + "':").needBlock();
				JSForm pa = null;
				for (String action : tam.fields.key2Set(field)) {
					JSForm ca = JSForm.flex("'" + action + "':").nestArray();
					ca.add(JSForm.flex(String.join(",", tam.fields.get(field, action))).noSemi());
					if (pa != null)
						pa.comma();
					curr.add(ca);
					pa = ca;
				}
				onUpdate.add(curr);
				prev = curr;
			}
			target.add(onUpdate);
		}
	}

	// rename this
	private void latestTemplateGeneration(JSTarget target, Template cg) {
		GeneratorContext cx = new GeneratorContext(target, cg);
		// TODO: when compatibility mode is over, I want to just make this "_render"
		JSForm ir = JSForm.flexFn(cx.protoName + "_initialRender", CollectionUtils.listOf("doc", "wrapper", "parent"));
		String topBlock = cx.nextArea();
		ir.add(JSForm.flex("new " + topBlock + "(new CardArea(parent, wrapper, this))"));
		target.add(ir);
		
		latestRecurse(cx, topBlock, cg.content);
	}

	// rename this
	private JSForm latestRecurse(GeneratorContext cx, String called, TemplateLine tl) {
		JSForm fn = JSForm.flex(called +" = function(parent)").needBlock();
		cx.target.add(fn);
		String base;
		String moreArgs = "";
		boolean isEditable = false;
		if (tl instanceof TemplateDiv) {
			TemplateDiv td = (TemplateDiv) tl;
			base = "DivArea";
			if (td.customTag != null)
				moreArgs = ", '" + td.customTag + "'";
			// TODO: custom tag var is hard & needs "assign" logic
		} else if (tl instanceof TemplateList) {
			base = "ListArea";
		} else if (tl instanceof ContentString || tl instanceof ContentExpr) {
			base = "TextArea";
			isEditable = tl instanceof ContentExpr && ((ContentExpr)tl).editable();
		} else {
			throw new UtilException("Template of type " + tl.getClass() + " not supported");
		}
		fn.add(JSForm.flex(base +".call(this, parent" + moreArgs + ")"));
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
						HSIEForm form = hsie.handleExpr(tea.value, CodeType.CARD);
						JSForm.assign(sak, "var attr", form);
						sak.add(JSForm.flex("attr = FLEval.full(attr)"));
						JSForm ifassign = JSForm.flex("if (attr && !(attr instanceof FLError))").needBlock();
						sak.add(ifassign);
//						ifassign.add(JSForm.flex("console.log('setting attribute " + tea.attr +" on', this._mydiv.id, 'to', attr)"));
						ifassign.add(JSForm.flex("this._mydiv.setAttribute('" + tea.attr +"', attr)"));
						cx.target.add(sak);
//						fn.add(JSForm.flex("this._setAttr_" + an +"()"));
						callOnAssign(fn, tea.value, saf);
						an++;
						break;
					}
					default:
						throw new UtilException("Cannot handle TEA type " + tea.type);
					}
				} else
					throw new UtilException("Cannot handle attr " + a.getClass());
			}
			for (EventHandler eh : td.handlers) {
				JSForm ahf = JSForm.flex(called +".prototype._add_handlers = function()").needBlock();
				HSIEForm expr = hsie.handleExpr(eh.expr, HSIEForm.CodeType.AREA);
				curry.rewrite(tc, expr);
				
				JSForm.assign(ahf, "var eh" + eh.action, expr);
				JSForm cev = JSForm.flex("this._mydiv['on" + eh.action + "'] = function(event)").needBlock();
				cev.add(JSForm.flex("this._area._wrapper.dispatchEvent(eh" + eh.action + ", event)"));
				ahf.add(cev);
				cx.target.add(ahf);

				callOnAssign(fn, eh.expr, called + ".prototype._add_handlers");
			}
			for (TemplateLine c : td.nested) {
				String v = cx.currentVar();
				String cn = cx.nextArea();
				fn.add(JSForm.flex("var " + v + " = new " + cn + "(this)"));
				latestRecurse(cx, cn, c);
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
			JSForm cfn = latestRecurse(cx, item, l.template);
			cfn.add(JSForm.flex("this._makeDraggable()"));
		} else if (tl instanceof ContentString) {
			ContentString cs = (ContentString) tl;
			fn.add(JSForm.flex("this._setText('" + cs.text + "')"));
		} else if (tl instanceof ContentExpr) {
			ContentExpr ce = (ContentExpr)tl;
			Object valExpr = ce.expr;
			callOnAssign(fn, valExpr, called + ".prototype._assignToText");
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
		} else {
			throw new UtilException("Template of type " + tl.getClass() + " not supported");
		}
		if (tl instanceof TemplateFormat) {
			handleFormats(cx, fn, isEditable, ((TemplateFormat)tl).formats);
		}
		if (newVar != null) {
			cx.removeLastCopyVar();
		}
		return fn;
	}

	protected void handleFormats(GeneratorContext cx, JSForm fn, boolean isEditable, List<Object> formats) {
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
				} else {
					System.out.println(tt);
					throw new UtilException("Cannot handle format of type " + tt.type);
				}
			} else if (o instanceof ApplyExpr) {
				// TODO: need to collect object/field pairs that we depend on
				ApplyExpr ae = (ApplyExpr) o;
				if (first == null)
					first = ae.location;
				if (expr == null)
					expr = cx.nil;
				expr = new ApplyExpr(null, cx.cons, o, expr);
			} else
				throw new UtilException("Cannot handle format of type " + o.getClass());
		}
		if (expr != null) {
			if (simple.length() > 0)
				expr = new ApplyExpr(null, cx.cons, new StringLiteral(null, simple.substring(1)), expr);
			// Basically we need to add a new function
			// Add code for "onAssign" for each of the variables mentioned in the list we haven't collected yet
			// But we don't have any test cases just yet
			throw new UtilException("variable format case");
//			ret.complexFormats = expr;
//			ret.sid = "sid" + nextId++;
		}
		else if (expr == null && simple.length() > 0) {
			fn.add(JSForm.flex("this._mydiv.className = '" + simple.substring(1) + "'"));
		}
	}
	
	private void createRules(GeneratorContext cx, String area, Object container, String field) {
		JSForm rules = JSForm.flex(area + "._rules =").needBlock();
		JSForm save = JSForm.flex("save: function(wrapper, text)").needBlock();
		if (container != null) {
			JSForm.assign(save, "var containingObject", hsie.handleExpr(container, CodeType.CARD));
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

	protected void callOnAssign(JSForm fn, Object valExpr, String call) {
		if (valExpr instanceof CardMember) {
			fn.add(JSForm.flex("this._onAssign(this._card, '" + ((CardMember)valExpr).var + "', " + call + ")"));
		} else if (valExpr instanceof TemplateListVar) {
			String var = ((TemplateListVar)valExpr).name;
			fn.add(JSForm.flex("this._src_" + var + "._interested(this, " + call + ")"));
			
		} else if (valExpr instanceof CardFunction || valExpr instanceof StringLiteral) {
			// nothing to do here, not variable
		} else if (valExpr instanceof ApplyExpr) {
			ApplyExpr ae = (ApplyExpr) valExpr;
			if (ae.fn instanceof AbsoluteVar && ((AbsoluteVar)ae.fn).id.equals("FLEval.field")) {
				Object expr = ae.args.get(0);
				if (expr instanceof TemplateListVar) {
					String name = ((TemplateListVar)expr).name;
					expr = "this._src_" + name + "." + name;
				} else 
					throw new UtilException("Handle apply expr: " + expr);
				String field = ((StringLiteral)ae.args.get(1)).text;
				fn.add(JSForm.flex("this._onAssign(" + expr +", '" + field + "', " + call + ")"));
			} else {
				callOnAssign(fn, ae.fn, call);
				for (Object o : ae.args)
					callOnAssign(fn, o, call);
			}
		} else
			throw new UtilException("Not handled: " + valExpr.getClass());
		fn.add(JSForm.flex(call + ".call(this)"));
	}

	private TemplateAbstractModel makeAbstractTemplateModel(ErrorResult errors, Rewriter rewriter, HSIE hsie, Template cg) {
		TemplateAbstractModel ret = new TemplateAbstractModel(cg.prefix, rewriter, cg.scope);
		matmRecursive(errors, hsie, ret, null, null, cg.content);
		return ret;
	}

	private void matmRecursive(ErrorResult errors, HSIE hsie, TemplateAbstractModel tam, AbstractTreeNode atn, VisualTree tree, TemplateLine content) {
		if (content instanceof TemplateDiv) {
			TemplateDiv td = (TemplateDiv) content;
			List<Handler> handlers = new ArrayList<Handler>();
			for (EventHandler eh : td.handlers) {
				HSIEForm expr = hsie.handleExpr(eh.expr, HSIEForm.CodeType.FUNCTION);
				curry.rewrite(tc, expr);
				handlers.add(new Handler(tam.ehId(), eh.action, expr));
			}
			org.flasck.flas.TemplateAbstractModel.Block b = tam.createBlock(errors, td.customTag, td.attrs, td.formats, handlers);
			b.sid = tam.nextSid();
			VisualTree vt = new VisualTree(b, null);
			if (atn == null) {
				atn = new AbstractTreeNode(AbstractTreeNode.TOP, null, null, null, vt);
				tam.nodes.add(atn);
				vt.containsThing = AbstractTreeNode.TOP;
			} else
				tree.children.add(vt);
			for (TemplateLine x : td.nested)
				matmRecursive(errors, hsie, tam, atn, vt, x);
			tam.cardMembersCause(vt, "assign", Generator.lname(tam.prefix, true) + "_formatTop");
		} else if (content instanceof TemplateList) {
			TemplateList tl = (TemplateList) content;
			org.flasck.flas.TemplateAbstractModel.Block b = tam.createBlock(errors, "ul", new ArrayList<Object>(), tl.formats, new ArrayList<Handler>());
			b.sid = tam.nextSid();
			VisualTree pvt = new VisualTree(b, null);
			pvt.divThing.listVar = ((CardMember)tl.listVar).var;
			pvt.divThing.supportDrag = tl.supportDragOrdering;
			pvt.containsThing = AbstractTreeNode.LIST;
			if (atn == null)
				tam.nodes.add(new AbstractTreeNode(AbstractTreeNode.TOP, null, null, null, pvt));
			else
				tree.children.add(pvt);
			tam.fields.add(((CardMember)tl.listVar).var, "assign", Generator.lname(tam.prefix, true) + "_" + b.id + "_assign");
			tam.fields.add(((CardMember)tl.listVar).var, "itemInserted", Generator.lname(tam.prefix, true) + "_" + b.id + "_itemInserted");
			tam.fields.add(((CardMember)tl.listVar).var, "itemChanged", Generator.lname(tam.prefix, true) + "_" + b.id + "_itemChanged");
			
			// This is where we separate the "included-in-parent" tree from the "I own this" tree
			VisualTree vt = new VisualTree(null, null);
			atn = new AbstractTreeNode(AbstractTreeNode.LIST, atn, b.id, b.sid, vt);
			atn.var = pvt.divThing.listVar;
			vt.draggable = tl.supportDragOrdering;
			tam.nodes.add(atn);

			// Now generate the nested template in that
			matmRecursive(errors, hsie, tam, atn, vt, tl.template);
			tam.cardMembersCause(vt, "assign", Generator.lname(tam.prefix, true) + "_" + b.id + "_formatList");
		} else if (content instanceof TemplateCases) {
			TemplateCases cases = (TemplateCases) content;
			org.flasck.flas.TemplateAbstractModel.Block b = tam.createBlock(errors, "div", new ArrayList<Object>(), new ArrayList<Object>(), new ArrayList<Handler>());
			b.sid = tam.nextSid();
			VisualTree pvt = new VisualTree(b, null);
			pvt.containsThing = AbstractTreeNode.CASES;
			if (atn == null)
				tam.nodes.add(new AbstractTreeNode(AbstractTreeNode.TOP, null, null, null, pvt));
			else
				tree.children.add(pvt);
			tam.cardMembersCause(cases.switchOn, "assign", Generator.lname(tam.prefix, true) + "_" + b.id + "_switch");
			
			// This is where we separate the "included-in-parent" tree from the "I own this" tree
			atn = new AbstractTreeNode(AbstractTreeNode.CASES, atn, b.id, b.sid, null);
			tam.nodes.add(atn);
			for (TemplateOr tor : cases.cases) {
				// Now generate each nested template in that
				VisualTree vt = new VisualTree(null, null);
				matmRecursive(errors, hsie, tam, atn, vt, tor.template);
				tam.cardMembersCause(tor.cond, "assign", Generator.lname(tam.prefix, true) + "_" + b.id + "_switch");
				atn.cases.add(new OrCase(hsie.handleExpr(new ApplyExpr(tor.location(), tam.scope.fromRoot(tor.location(), "=="), cases.switchOn, tor.cond), HSIEForm.CodeType.CARD), vt));
			}
		} else if (content instanceof ContentString) {
			ContentString cs = (ContentString) content;
			org.flasck.flas.TemplateAbstractModel.Block b = tam.createBlock(errors, "span", new ArrayList<Object>(), cs.formats, new ArrayList<Handler>());
			VisualTree vt = new VisualTree(b, cs.text);
			if (atn == null)
				tam.nodes.add(new AbstractTreeNode(AbstractTreeNode.TOP, null, null, null, vt));
			else
				tree.children.add(vt);
		} else if (content instanceof ContentExpr) {
			ContentExpr ce = (ContentExpr) content;
			org.flasck.flas.TemplateAbstractModel.Block b = tam.createBlock(errors, "span", new ArrayList<Object>(), ce.formats, new ArrayList<Handler>());
			b.sid = tam.nextSid();
			VisualTree pvt = new VisualTree(b, null, ce.editable());
			pvt.containsThing = AbstractTreeNode.CONTENT;
			if (atn == null)
				tam.nodes.add(new AbstractTreeNode(AbstractTreeNode.TOP, null, null, null, pvt));
			else
				tree.children.add(pvt);
			tam.cardMembersCause(ce.expr, "assign", Generator.lname(tam.prefix, true) + "_" + b.id);
			
			// Now we need to create a new ATN for the _content_ function
			// VisualTree vt = new VisualTree(null);
			atn = new AbstractTreeNode(AbstractTreeNode.CONTENT, atn, b.id, b.sid, null);
			tam.nodes.add(atn);
			atn.expr = hsie.handleExpr(ce.expr, HSIEForm.CodeType.CARD);
			if (ce.editable()) {
				atn.editable = ce.editable();
				if (ce.expr instanceof CardMember) {
					atn.editfield = ((CardMember)ce.expr).var;
				} else if (ce.expr instanceof ApplyExpr) {
					ApplyExpr ae = (ApplyExpr) ce.expr;
					if (!(ae.fn instanceof AbsoluteVar) || !(((AbsoluteVar)ae.fn).id.equals("FLEval.field")))
						throw new UtilException("Invalid expr for edit field " + ae.fn);
					atn.editobject = hsie.handleExpr(ae.args.get(0), HSIEForm.CodeType.CARD);
					atn.editfield = ((StringLiteral)ae.args.get(1)).text;
				} else
					throw new UtilException("Do not know how to/you should not be able to edit a field of type " + ce.expr.getClass());
			}
		} else if (content instanceof CardReference) {
			CardReference card = (CardReference) content;
			org.flasck.flas.TemplateAbstractModel.Block b = tam.createBlock(errors, "div", new ArrayList<Object>(), new ArrayList<Object>(), new ArrayList<Handler>());
			b.sid = tam.nextSid();
			VisualTree pvt = new VisualTree(b, null);
			pvt.containsThing = AbstractTreeNode.CARD;
			if (atn == null)
				tam.nodes.add(new AbstractTreeNode(AbstractTreeNode.TOP, null, null, null, pvt));
			else
				tree.children.add(pvt);
			
			atn = new AbstractTreeNode(AbstractTreeNode.CARD, atn, b.id, b.sid, null);
			tam.nodes.add(atn);
			atn.card = card;
		} else if (content instanceof D3Invoke) {
			D3Invoke d3i = (D3Invoke) content;
			org.flasck.flas.TemplateAbstractModel.Block b = tam.createBlock(errors, "div", new ArrayList<Object>(), new ArrayList<Object>(), new ArrayList<Handler>());
			b.sid = tam.nextSid();
			VisualTree pvt = new VisualTree(b, null);
			pvt.containsThing = AbstractTreeNode.D3;
			pvt.divThing.name = d3i.d3.name;
			if (atn == null)
				tam.nodes.add(new AbstractTreeNode(AbstractTreeNode.TOP, null, null, null, pvt));
			else
				tree.children.add(pvt);
			tam.fields.add(((CardMember)d3i.d3.data).var, "assign", Generator.lname(tam.prefix, true) + "_" + b.id);
			
			atn = new AbstractTreeNode(AbstractTreeNode.D3, atn, b.id, b.sid, null);
			tam.nodes.add(atn);
			atn.d3 = d3i;
		} else 
			throw new UtilException("TL type " + content.getClass() + " not supported");
	}
	
	public void generate(JSTarget target, TemplateAbstractModel tam, JSForm function, AbstractTreeNode parent) {
		for (AbstractTreeNode atn : tam.nodes) {
			if (atn.nestedIn != parent)
				continue;
			if (atn.type == AbstractTreeNode.TOP) {
				JSForm ir = JSForm.flexFn(Generator.lname(tam.prefix, true) + "_initialRender", CollectionUtils.listOf("doc", "wrapper", "parent"));
				target.add(ir);
				generateVisualTree(ir, "parent", false, null, atn.tree);
				generate(target, tam, null, atn);
				JSForm ft = JSForm.flexFn(Generator.lname(tam.prefix, true) + "_formatTop", CollectionUtils.listOf("doc", "wrapper"));
				target.add(ft);
				generateFormatsFor(ft, atn.tree, "wrapper.infoAbout");
			} else if (atn.type == AbstractTreeNode.LIST) {
				{
					JSForm ra = JSForm.flexFn(Generator.lname(tam.prefix, true) + "_" + atn.id + "_assign", CollectionUtils.listOf("doc", "wrapper"));
					target.add(ra);
					ra.add(JSForm.flex("wrapper.infoAbout['" + atn.id + "'] = {}"));
					String lv = atn.var;
					ra.add(JSForm.flex("this." + lv + " = FLEval.full(this." + lv +")"));
					ra.add(JSForm.flex("this._" + atn.id + "_clear(doc, wrapper)"));
					JSForm ifnonnull = JSForm.flex("if (this." + lv +")").needBlock();
					ra.add(ifnonnull);
					ifnonnull.add(JSForm.flex("if (this." + lv + "._ctor !== 'Croset') throw new Error('List logic only handles Crosets right now')"));
					ifnonnull.add(JSForm.flex("var r = this." + lv + ".range(0, 10)")); // TODO: 0, 10 here is a hack
					JSForm loop = JSForm.flex("while (r._ctor == 'Cons')").needBlock();
					loop.add(JSForm.flex("var v = r.head"));
					loop.add(JSForm.flex("this._" + atn.id + "_itemInserted(doc, wrapper, v, null)"));
					loop.add(JSForm.flex("this._" + atn.id + "_itemChanged(doc, wrapper, v)"));
					loop.add(JSForm.flex("r = r.tail"));
					ifnonnull.add(loop);
					ra.add(JSForm.flex("this._" + atn.id + "_formatList(doc, wrapper)"));

				}
				JSForm ii = JSForm.flexFn(Generator.lname(tam.prefix, true) + "_" + atn.id + "_itemInserted", CollectionUtils.listOf("doc", "wrapper", "item", "before"));
				target.add(ii);
				ii.add(JSForm.flex("var parent = doc.getElementById(wrapper.infoAbout['" + atn.sid + "'])"));
				ii.add(JSForm.flex("wrapper.infoAbout['" + atn.id + "'][item.id] = { item: item }"));
				for (VisualTree t : atn.tree.children) {
					if (atn.tree.draggable)
						t.draggable = true;
					generateVisualTree(ii, "parent", true, atn.id, t);
				}
				ii.add(JSForm.flex("this._" + atn.id + "_formatItem(doc, wrapper, wrapper.infoAbout['" + atn.id + "'][item.id])"));
				JSForm cl = JSForm.flexFn(Generator.lname(tam.prefix, true) + "_" + atn.id + "_clear", CollectionUtils.listOf("doc", "wrapper"));
				target.add(cl);
				cl.add(JSForm.flex("var " + atn.id + " = doc.getElementById(wrapper.infoAbout['" + atn.sid + "'])"));
				cl.add(JSForm.flex(atn.id + ".innerHTML = ''"));
				JSForm ic = JSForm.flexFn(Generator.lname(tam.prefix, true) + "_" + atn.id + "_itemChanged", CollectionUtils.listOf("doc", "wrapper", "item"));
				target.add(ic);
				generate(target, tam, ic, atn);
				JSForm fi = JSForm.flexFn(Generator.lname(tam.prefix, true) + "_" + atn.id + "_formatItem", CollectionUtils.listOf("doc", "wrapper", "info"));
				target.add(fi);
				for (VisualTree t : atn.tree.children)
					generateFormatsFor(fi, t, "info");
				JSForm fl = JSForm.flexFn(Generator.lname(tam.prefix, true) + "_" + atn.id + "_formatList", CollectionUtils.listOf("doc", "wrapper"));
				target.add(fl);
				JSForm lp = JSForm.flex("for (var x in wrapper.infoAbout['" + atn.id + "'])").needBlock();
				fl.add(lp);
				lp.add(JSForm.flex("this._" + atn.id + "_formatItem(doc, wrapper, wrapper.infoAbout['" + atn.id + "'][x])"));
			} else if (atn.type == AbstractTreeNode.CASES) {
				JSForm sw = JSForm.flexFn(Generator.lname(tam.prefix, true) + "_" + atn.id + "_switch", CollectionUtils.listOf("doc", "wrapper"));
				target.add(sw);
				sw.add(JSForm.flex("var " + atn.id + " = doc.getElementById(wrapper.infoAbout['" + atn.sid + "'])"));
				sw.add(JSForm.flex(atn.id + ".innerHTML = ''"));
				sw.add(JSForm.flex("var cond"));
				for (OrCase oc : atn.cases) {
					JSForm.assign(sw, "cond", oc.expr);
					JSForm doit = JSForm.flex("if (FLEval.full(cond))").needBlock();
					for (VisualTree t : oc.tree.children)
						generateVisualTree(doit, atn.id, false, null, t);
					doit.add(JSForm.flex("return"));
					sw.add(doit);
				}
				
				// Generate all the things we're calling
				generate(target, tam, null, atn);
			} else if (atn.type == AbstractTreeNode.CONTENT) {
				JSForm cc = function;
				if (cc == null) {
					cc = JSForm.flexFn(Generator.lname(tam.prefix, true) + "_" + atn.id, CollectionUtils.listOf("doc", "wrapper"));
					target.add(cc);
				}
				cc.add(JSForm.flex("var span = doc.getElementById(wrapper.infoAbout" + (atn.nestedIn != null && atn.nestedIn.id != null?"['" + atn.nestedIn.id + "'][item.id]":"") + "['" + atn.sid + "'])"));
				cc.add(JSForm.flex("span.innerHTML = ''"));
				JSForm.assign(cc, "var textContent", atn.expr);
				cc.add(JSForm.flex("var text = doc.createTextNode(FLEval.full(textContent))"));
				cc.add(JSForm.flex("span.appendChild(text)"));
				if (atn.editable) {
					JSForm rules = JSForm.flex(Generator.lname(tam.prefix, true) + "_" + atn.id + "_rules =").needBlock();
					String inside = "b"; // This gratuituous hack should read "the iteration var we need in assign" which will be bound to "item" in the call
					JSForm save = JSForm.flex("save: function(wrapper, " + inside + ", text)").needBlock();
					String containingObject = "this";
					if (atn.editobject != null) {
						containingObject = "saveTo";
						JSForm.assign(save, "var saveTo", atn.editobject);
					}
					// TODO: we may need to convert the text field to a more complex object type (e.g. integer) as specified in the rules we are given
					save.add(JSForm.flex(containingObject + "." + atn.editfield + " = text"));
					// TODO: we need to consider which of the four types of change was just made (based on something put on atn)
					// 1. Transient local state (do nothing more)
					// 2. Persistent local state (save state object)
					// 3. Main object field or 4. Loaded object field (save data object using the appropriate contract)
					save.add(JSForm.flex("wrapper.saveObject(" + containingObject + ")"));
					save.add(JSForm.flex("console.log('saved to:', " + containingObject + ")"));
					rules.add(save);
					// if we add another block, need "save.comma();"
					target.add(rules);
				}
			} else if (atn.type == AbstractTreeNode.CARD) {
				JSForm cc = function;
				if (cc == null) {
					cc = JSForm.flexFn(Generator.lname(tam.prefix, true) + "_" + atn.id, CollectionUtils.listOf("doc", "wrapper"));
					target.add(cc);
				}
				cc.add(JSForm.flex("wrapper.showCard('" + atn.sid + "', { card: " + atn.card.explicitCard + "})"));
			} else if (atn.type == AbstractTreeNode.D3) {
				JSForm cc = function;
				if (cc == null) {
					cc = JSForm.flexFn(Generator.lname(tam.prefix, true) + "_" + atn.id, CollectionUtils.listOf("doc", "wrapper"));
					target.add(cc);
				}
				cc.add(JSForm.flex("var slot = wrapper.infoAbout['" + atn.sid + "']"));
				cc.add(JSForm.flex("var info = wrapper.infoAbout['" + atn.id + "']"));
				cc.add(JSForm.flex("wrapper.updateD3(slot, info)"));
			} else
				throw new UtilException("Don't handle " + atn.type);
		}
	}

	private void generateFormatsFor(JSForm fi, VisualTree t, String var) {
		if (t.divThing.complexFormats != null) {
			fi.add(JSForm.flex("var item = " + var + ".item"));
			HSIEForm form = hsie.handleExpr(t.divThing.complexFormats, CodeType.CARD);
			JSForm.assign(fi, "var cls", form);
			fi.add(JSForm.flex("doc.getElementById(" + var + "['" + t.divThing.sid + "']).setAttribute('class', join(FLEval.full(cls), ' '))"));
		}
		for (Entry<String, Object> x : t.divThing.exprAttrs.entrySet()) {
			fi.add(JSForm.flex("var item = " + var + ".item"));
			HSIEForm form = hsie.handleExpr(x.getValue(), CodeType.CARD);
			JSForm.assign(fi, "var attr", form);
			fi.add(JSForm.flex("attr = FLEval.full(attr)"));
			JSForm ifassign = JSForm.flex("if (attr)").needBlock();
			fi.add(ifassign);
			ifassign.add(JSForm.flex("doc.getElementById(" + var + "['" + t.divThing.sid + "']).setAttribute('" + x.getKey() + "', attr)"));
		}
		for (VisualTree c : t.children)
			generateFormatsFor(fi, c, var);
	}

	private void generateVisualTree(JSForm ir, String parent, boolean considerBefore, String inList, VisualTree tree) {
		if (tree.containsThing != AbstractTreeNode.D3) {
			if (tree.divThing.ns == null)
				ir.add(JSForm.flex("var " + tree.divThing.id + " = doc.createElement('" + tree.divThing.tag + "')"));
			else
				ir.add(JSForm.flex("var " + tree.divThing.id + " = doc.createElementNS('" + tree.divThing.ns + "', '" + tree.divThing.tag + "')"));
		}
		if (tree.divThing.sid != null) {
			ir.add(JSForm.flex("var " + tree.divThing.sid + " = wrapper.nextSlotId()"));
			if (tree.containsThing != AbstractTreeNode.D3) {
				ir.add(JSForm.flex(tree.divThing.id + ".setAttribute('id', " + tree.divThing.sid + ")"));
			} else
				ir.add(JSForm.flex(parent + ".setAttribute('id', " + tree.divThing.sid + ")"));
			ir.add(JSForm.flex("wrapper.infoAbout" + (inList != null?"['" + inList + "'][item.id]":"")  + "['" + tree.divThing.sid + "'] = " + tree.divThing.sid));
			if (tree.editable)
				ir.add(JSForm.flex("wrapper.editableField(" + tree.divThing.id + ", this._" + tree.divThing.id + "_rules, " +(inList != null?"item":"this") + ")"));
		}
		if (tree.divThing.supportDrag) {
			JSForm dragover = JSForm.flex(tree.divThing.id + "['ondragover'] = function(event)").needBlock();
			dragover.add(JSForm.flex("_Croset.listDragOver(event, " + tree.divThing.sid + ")"));
			ir.add(dragover);
			JSForm drop = JSForm.flex(tree.divThing.id + "['ondrop'] = function(event)").needBlock();
			drop.add(JSForm.flex("_Croset.listDrop(event, " + tree.divThing.sid + ")"));
			ir.add(drop);
			ir.add(JSForm.flex(tree.divThing.id + "['croset'] = this." + tree.divThing.listVar));
		}
		if (tree.draggable) {
			ir.add(JSForm.flex(tree.divThing.id + ".setAttribute('draggable', 'true')"));
			JSForm drag = JSForm.flex(tree.divThing.id + "['ondragstart'] = function(event)").needBlock();
			drag.add(JSForm.flex("_Croset.listDrag(event)"));
			ir.add(drag);
			ir.add(JSForm.flex(tree.divThing.id + "['item_id'] = item.id"));
		}
		for (Entry<String, String> sa : tree.divThing.staticAttrs.entrySet())
			ir.add(JSForm.flex(tree.divThing.id+".setAttribute('" + sa.getKey() +"', '" + sa.getValue() + "')"));
		if (tree.containsThing != AbstractTreeNode.D3) {
			JSForm ip = ir;
			if (considerBefore) {
				JSForm ie = JSForm.flex("if (before)").needBlock();
				ir.add(ie);
				ie.add(JSForm.flex(parent + ".insertBefore(" + tree.divThing.id + ", before)"));
				JSForm es = JSForm.flex("else").needBlock();
				ir.add(es);
				ip = es;
			}
			ip.add(JSForm.flex(parent + ".appendChild(" + tree.divThing.id + ")"));
		}
//		if (tree.divThing.complexAttrs)
//			ir.add(JSForm.flex("wrapper.infoAbout['" + struct + "']" + (inList?"[item.id]":"")  + "['" + tree.divThing.sid +"'] = " + tree.divThing.sid));
		for (Handler eh : tree.divThing.handlers) {
			JSForm.assign(ir, "var eh" + eh.id, eh.code);
			JSForm cev = JSForm.flex(tree.divThing.id + "['on" + eh.on + "'] = function(event)").needBlock();
			cev.add(JSForm.flex("wrapper.dispatchEvent(eh" + eh.id + ", event)"));
			ir.add(cev);
		}
		if (tree.text != null) {
			ir.add(JSForm.flex(tree.divThing.id +".appendChild(doc.createTextNode('" + tree.text + "'))"));
		}
		for (VisualTree t : tree.children)
			generateVisualTree(ir, tree.divThing.id, false, inList, t);
		// TODO: surely this should only happen at the top level of calling the tree?
		// Does that work because of the setting of containsThing?
		if (tree.containsThing == AbstractTreeNode.TOP) {
			ir.add(JSForm.flex("this._formatTop(doc, wrapper)"));
		} else if (tree.containsThing == AbstractTreeNode.CONTENT) {
			if (inList == null)
				ir.add(JSForm.flex("this._" + tree.divThing.id + "(doc, wrapper)"));
		} else if (tree.containsThing == AbstractTreeNode.LIST) {
			ir.add(JSForm.flex("this._" + tree.divThing.id + "_assign(doc, wrapper)"));
			/*
			ir.add(JSForm.flex("wrapper.infoAbout['" + tree.divThing.id + "'] = {}"));
			String lv = tree.divThing.listVar;
			ir.add(JSForm.flex("this." + lv + " = FLEval.full(this." + lv +")"));
			JSForm ifnonnull = JSForm.flex("if (this." + lv +")").needBlock();
			ir.add(ifnonnull);
			JSForm loop = JSForm.flex("for (var k=0;k<this." + lv + ".members.length;k++)").needBlock();
			loop.add(JSForm.flex("var v = this." + lv + ".members[k]"));
			loop.add(JSForm.flex("this._" + tree.divThing.id + "_itemInserted(doc, wrapper, v.value, null)"));
			loop.add(JSForm.flex("this._" + tree.divThing.id + "_itemChanged(doc, wrapper, v.value)"));
			ifnonnull.add(loop);
			ir.add(JSForm.flex("this._" + tree.divThing.id + "_formatList(doc, wrapper)"));
			*/
		} else if (tree.containsThing == AbstractTreeNode.CASES) {
			ir.add(JSForm.flex("wrapper.infoAbout['" + tree.divThing.id + "'] = {}"));
			ir.add(JSForm.flex("this._" + tree.divThing.id + "_switch(doc, wrapper)"));
		} else if (tree.containsThing == AbstractTreeNode.CARD) {
//			ir.add(JSForm.flex("wrapper.infoAbout['" + tree.divThing.id + "'] = {}"));
			ir.add(JSForm.flex("this._" + tree.divThing.id + "(doc, wrapper)"));
		} else if (tree.containsThing == AbstractTreeNode.D3) {
			ir.add(JSForm.flex("wrapper.infoAbout['" + tree.divThing.id + "'] = FLEval.full(this._d3init_" + tree.divThing.name + "())"));
			ir.add(JSForm.flex("this._" + tree.divThing.id +"(doc, wrapper)"));
		}
	}

}
