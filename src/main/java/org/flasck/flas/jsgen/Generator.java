package org.flasck.flas.jsgen;

import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;

import org.flasck.flas.TemplateAbstractModel;
import org.flasck.flas.TemplateAbstractModel.AbstractTreeNode;
import org.flasck.flas.TemplateAbstractModel.Base;
import org.flasck.flas.TemplateAbstractModel.Block;
import org.flasck.flas.TemplateAbstractModel.Content;
import org.flasck.flas.TemplateAbstractModel.Struct;
import org.flasck.flas.TemplateAbstractModel.ULList;
import org.flasck.flas.TemplateAbstractModel.VisualTree;
import org.flasck.flas.dom.RenderTree.Element;
import org.flasck.flas.dom.UpdateTree.Update;
import org.flasck.flas.errors.ErrorResult;
import org.flasck.flas.hsie.HSIE;
import org.flasck.flas.jsform.JSForm;
import org.flasck.flas.jsform.JSTarget;
import org.flasck.flas.parsedForm.CardGrouping;
import org.flasck.flas.parsedForm.CardGrouping.ContractGrouping;
import org.flasck.flas.parsedForm.CardGrouping.ServiceGrouping;
import org.flasck.flas.parsedForm.ContractImplements;
import org.flasck.flas.parsedForm.ContractService;
import org.flasck.flas.parsedForm.HandlerImplements;
import org.flasck.flas.parsedForm.StructDefn;
import org.flasck.flas.parsedForm.StructField;
import org.flasck.flas.vcode.hsieForm.BindCmd;
import org.flasck.flas.vcode.hsieForm.ErrorCmd;
import org.flasck.flas.vcode.hsieForm.HSIEBlock;
import org.flasck.flas.vcode.hsieForm.HSIEForm;
import org.flasck.flas.vcode.hsieForm.HSIEForm.Type;
import org.flasck.flas.vcode.hsieForm.Head;
import org.flasck.flas.vcode.hsieForm.IFCmd;
import org.flasck.flas.vcode.hsieForm.ReturnCmd;
import org.flasck.flas.vcode.hsieForm.Switch;
import org.flasck.flas.vcode.hsieForm.Var;
import org.zinutils.collections.CollectionUtils;
import org.zinutils.collections.ListMap;
import org.zinutils.exceptions.UtilException;

public class Generator {
	private final ErrorResult errors;
	private final JSTarget target;

	public Generator(ErrorResult errors, JSTarget target) {
		this.errors = errors;
		this.target = target;
	}
	
	public void generate(HSIEForm input) {
		String jsname = input.fnName;
		if (input.isMethod()) {
			int idx = jsname.lastIndexOf(".");
			jsname = jsname.substring(0, idx+1) + "prototype" + jsname.substring(idx);
			if (input.mytype == Type.HANDLER) {
				idx = jsname.lastIndexOf('.', idx-1);
			} else {
				idx = jsname.lastIndexOf("._C");
				if (idx == -1) idx = jsname.lastIndexOf("._S");
			}
			if (idx != -1) jsname = jsname.substring(0, idx+1) + "_" + jsname.substring(idx+1);
		}
		JSForm ret = JSForm.function(jsname, input.vars, input.alreadyUsed, input.nformal);
		generateBlock(input.fnName, input, ret, input);
		target.add(ret);
	}

	public void generate(StructDefn sd) {
		if (!sd.generate)
			return;
		int idx = sd.typename.lastIndexOf(".");
		String uname = sd.typename.substring(0, idx+1) + "_" + sd.typename.substring(idx+1);
		JSForm ret = JSForm.function(uname, CollectionUtils.listOf(new Var(0)), 0, 1);
		ret.add(new JSForm("this._ctor = '" + sd.typename + "'"));
		if (!sd.fields.isEmpty()) {
			JSForm ifBlock = new JSForm("if (v0)");
			ret.add(ifBlock);
			JSForm elseBlock = new JSForm("else").needBlock();
			ret.add(elseBlock);
			for (StructField x : sd.fields) {
				JSForm assign = new JSForm("if (v0."+x.name+")");
				assign.add(new JSForm("this."+x.name+" = v0."+x.name));
				ifBlock.add(assign);
				if (x.init != null) {
					JSForm defass = new JSForm("else");
					ifBlock.add(defass);
					HSIEForm form = new HSIE(errors).handleExpr(x.init, Type.FUNCTION);
//					form.dump();
					generateField(defass, x.name, form);
					generateField(elseBlock, x.name, form);
				}
			}
		}
		target.add(ret);

		List<Var> vars = new ArrayList<Var>();
		List<String> fields = new ArrayList<String>();
		int vi = 0;
		for (StructField sf : sd.fields) {
			Var v = new Var(vi++);
			vars.add(v);
			fields.add(sf.name+": "+ v);
		}
		JSForm ctor = JSForm.function(sd.typename, vars, 0, vars.size());
		ctor.add(new JSForm("return new " + uname + "({" + String.join(", ", fields) + "})"));
		target.add(ctor);
	}

	private void generateField(JSForm defass, String field, HSIEForm form) {
		if (form == null)
			defass.add(new JSForm("this."+ field + " = undefined"));
		else
			JSForm.assign(defass, "this." + field, form);
	}

	public void generate(String name, CardGrouping card) {
		JSForm cf = JSForm.function(name, CollectionUtils.listOf(new Var(0)), 0, 1);
		cf.add(new JSForm("var _self = this"));
		cf.add(new JSForm("this._ctor = '" + name + "'"));
		cf.add(new JSForm("this._wrapper = v0.wrapper"));
		cf.add(new JSForm("this._special = 'card'"));
		for (Entry<String, Object> x : card.inits.entrySet()) {
			HSIEForm form = null;
			if (x.getValue() != null) {
				form = new HSIE(errors).handleExpr(x.getValue(), Type.FUNCTION);
//					form.dump();
			}

			generateField(cf, x.getKey(), form);
		}
		cf.add(new JSForm("this._services = {}"));
		for (ServiceGrouping cs : card.services) {
			cf.add(new JSForm("this._services['" + cs.type + "'] = " + cs.implName + ".apply(this)"));
			if (cs.referAsVar != null)
				cf.add(new JSForm("this." + cs.referAsVar + " = this._services['" + cs.type + "']"));
		}
		cf.add(new JSForm("this._contracts = {}"));
		for (ContractGrouping ci : card.contracts) {
			cf.add(new JSForm("this._contracts['" + ci.type +"'] = "+ ci.implName + ".apply(this)"));
			if (ci.referAsVar != null)
				cf.add(new JSForm("this." + ci.referAsVar + " = this._contracts['" + ci.type + "']"));
		}
		target.add(cf);
	}

	public void generateContract(String ctorName, ContractImplements ci) {
		String clzname = ctorName.replace("._C", ".__C");
		JSForm clz = JSForm.function(clzname, CollectionUtils.listOf(new Var(0)), 0, 1);
		clz.add(new JSForm("this._ctor = '" + ctorName + "'"));
		clz.add(new JSForm("this._card = v0"));
		clz.add(new JSForm("this._special = 'contract'"));
		clz.add(new JSForm("this._contract = '" + ci.type + "'"));
		target.add(clz);

		JSForm ctor = JSForm.function(ctorName, new ArrayList<Var>(), 0, 0);
		ctor.add(new JSForm("return new " + clzname + "(this)"));
		target.add(ctor);
	}

	public void generateService(String ctorName, ContractService cs) {
		String clzname = ctorName.replace("._S", ".__S");
		JSForm clz = JSForm.function(clzname, CollectionUtils.listOf(new Var(0)), 0, 1);
		clz.add(new JSForm("this._ctor = '" + ctorName + "'"));
		clz.add(new JSForm("this._card = v0"));
		clz.add(new JSForm("this._special = 'service'"));
		clz.add(new JSForm("this._contract = '" + cs.type + "'"));
		target.add(clz);

		JSForm ctor = JSForm.function(ctorName, new ArrayList<Var>(), 0, 0);
		ctor.add(new JSForm("return new " + clzname + "(this)"));
		target.add(ctor);
	}

	public void generateHandler(String ctorName, HandlerImplements hi) {
		int idx = ctorName.lastIndexOf('.');
		String clzname = ctorName.substring(0, idx+1) + "_" + ctorName.substring(idx+1);
		List<Var> vars = new ArrayList<Var>();
		for (int i=0;i<=hi.boundVars.size();i++)
			vars.add(new Var(i));
		JSForm clz = JSForm.function(clzname, vars, 0, hi.boundVars.size() + 1);
		clz.add(new JSForm("this._ctor = '" + ctorName + "'"));
		clz.add(new JSForm("this._card = v0"));
		clz.add(new JSForm("this._special = 'handler'"));
		clz.add(new JSForm("this._contract = '" + hi.type + "'"));
		int v = 1;
		for (String s : hi.boundVars) 
			clz.add(new JSForm("this." + s + " = v" + v++));
		target.add(clz);

		JSForm ctor = JSForm.function(ctorName, vars, 0, hi.boundVars.size());
		StringBuffer sb = new StringBuffer("this");
		vars.remove(vars.size()-1);
		for (Var vi : vars)
			sb.append(", " + vi);
		ctor.add(new JSForm("return new " + clzname + "(" + sb +")"));
		target.add(ctor);
	}

	private void generateBlock(String fn, HSIEForm form, JSForm into, HSIEBlock input) {
		for (HSIEBlock h : input.nestedCommands()) {
//			System.out.println(h.getClass());
			if (h instanceof Head) {
				into.addAll(JSForm.head(((Head)h).v));
			} else if (h instanceof Switch) {
				Switch s = (Switch)h;
				JSForm sw = JSForm.switchOn(s.ctor, s.var);
				generateBlock(fn, form, sw, s);
				into.add(sw);
			} else if (h instanceof IFCmd) {
				IFCmd c = (IFCmd)h;
				JSForm b = JSForm.ifCmd(c);
				generateBlock(fn, form, b, c);
				into.add(b);
			} else if (h instanceof BindCmd) {
				into.add(JSForm.bind((BindCmd) h));
			} else if (h instanceof ReturnCmd) {
				ReturnCmd r = (ReturnCmd) h;
				into.addAll(JSForm.ret(r, form));
			} else if (h instanceof ErrorCmd) {
				into.add(JSForm.error(fn));
			} else {
				System.out.println("Cannot generate block:");
				h.dumpOne(0);
			}
		}
	}

	public void generate(TemplateAbstractModel tam, JSForm function, AbstractTreeNode parent) {
		for (AbstractTreeNode atn : tam.nodes) {
			if (atn.nestedIn != parent)
				continue;
			if (atn.type == AbstractTreeNode.TOP) {
				JSForm ir = JSForm.flexFn(tam.prefix + ".prototype._initialRender", CollectionUtils.listOf("doc", "wrapper", "parent"));
				target.add(ir);
				generateVisualTree(ir, "parent", false, null, atn.tree);
				generate(tam, null, atn);
			} else if (atn.type == AbstractTreeNode.LIST) {
				JSForm ii = JSForm.flexFn(tam.prefix + ".prototype._" + atn.id + "_itemInserted", CollectionUtils.listOf("doc", "wrapper", "item", "before"));
				target.add(ii);
				ii.add(JSForm.flex("var parent = doc.getElementById(wrapper.infoAbout['" + atn.sid + "'])"));
				ii.add(JSForm.flex("wrapper.infoAbout['" + atn.id + "'][item.id] = { item: item }"));
				for (VisualTree t : atn.tree.children)
					generateVisualTree(ii, "parent", true, atn.id, t);
				ii.add(JSForm.flex("this._" + atn.id + "_formatItem(doc, wrapper, wrapper.infoAbout['" + atn.id + "'][item.id])"));
				JSForm cl = JSForm.flexFn(tam.prefix + ".prototype._" + atn.id + "_clear", CollectionUtils.listOf("doc", "wrapper"));
				target.add(cl);
				cl.add(JSForm.flex("var " + atn.id + " = doc.getElementById(wrapper.infoAbout['" + atn.sid + "'])"));
				cl.add(JSForm.flex(atn.id + ".innerHTML = ''"));
				JSForm ic = JSForm.flexFn(tam.prefix + ".prototype._" + atn.id + "_itemChanged", CollectionUtils.listOf("doc", "wrapper", "item"));
				target.add(ic);
				generate(tam, ic, atn);
				JSForm fi = JSForm.flexFn(tam.prefix + ".prototype._" + atn.id + "_formatItem", CollectionUtils.listOf("doc", "wrapper", "info"));
				target.add(fi);
				for (VisualTree t : atn.tree.children)
					generateFormatsFor(fi, t, "info");
				JSForm fl = JSForm.flexFn(tam.prefix + ".prototype._" + atn.id + "_formatList", CollectionUtils.listOf("doc", "wrapper"));
				target.add(fl);
				JSForm lp = JSForm.flex("for (var x in wrapper.infoAbout['" + atn.id + "'])").needBlock();
				fl.add(lp);
				lp.add(JSForm.flex("this._" + atn.id + "_formatItem(doc, wrapper, wrapper.infoAbout['" + atn.id + "'][x])"));
			} else if (atn.type == AbstractTreeNode.CONTENT) {
				JSForm cc = function;
				if (cc == null) {
					cc = JSForm.flexFn(tam.prefix + ".prototype._" + atn.id, CollectionUtils.listOf("doc", "wrapper", "value"));
					target.add(cc);
				}
				cc.add(JSForm.flex("var span = doc.getElementById(wrapper.infoAbout" + (atn.nestedIn != null?"['" + atn.nestedIn.id + "'][item.id]":"") + "['" + atn.sid + "'])"));
				cc.add(JSForm.flex("span.innerHTML = ''"));
				JSForm.assign(cc, "var textContent", atn.expr);
				cc.add(JSForm.flex("var text = doc.createTextNode(FLEval.full(textContent))"));
				cc.add(JSForm.flex("span.appendChild(text)"));
			} else
				throw new UtilException("Don't handle " + atn.type);
		}
		/*
		// Firstly firstly, create the "initialRender" function
		JSForm ir = JSForm.flexFn(tam.prefix + ".initialRender", CollectionUtils.listOf("doc", "wrapper", "parent", "card"));
		
		// first prepare any infos
		for (Base c : tam.root.children) {
			if (c instanceof ULList) {
				ir.add(JSForm.flex("wrapper.infoAbout['" + c.id + "'] = {}"));
			}
		}
		createSomething(ir, tam.root);
		for (Base c : tam.root.children) {
			createSomething(ir, c);
		}
		target.add(ir);
		
		// Now do the actual functions
		genFunction(tam, tam.root);
		for (Base c : tam.root.children) {
			genFunction(tam, c);
		}
		
		// Generate the update actions
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
		*/
	}

	private void generateFormatsFor(JSForm fi, VisualTree t, String var) {
		if (t.divThing.complexAttrs != null) {
			fi.add(JSForm.flex("var item = " + var + ".item"));
			HSIEForm form = new HSIE(errors).handleExpr(t.divThing.complexAttrs, Type.CARD);
			JSForm.assign(fi, "var attrs", form);
			fi.add(JSForm.flex("doc.getElementById(" + var + "['" + t.divThing.sid + "']).setAttribute('class', join(FLEval.full(attrs), ' '))"));
		}
		for (VisualTree c : t.children)
			generateFormatsFor(fi, c, var);
	}

	private void generateVisualTree(JSForm ir, String parent, boolean considerBefore, String inList, VisualTree tree) {
		ir.add(JSForm.flex("var " + tree.divThing.id + " = doc.createElement('" + tree.divThing.tag + "')"));
		if (tree.divThing.sid != null) {
			ir.add(JSForm.flex("var " + tree.divThing.sid + " = wrapper.nextSlotId()"));
			ir.add(JSForm.flex(tree.divThing.id + ".setAttribute('id', " + tree.divThing.sid + ")"));
			ir.add(JSForm.flex("wrapper.infoAbout" + (inList != null?"['" + inList + "'][item.id]":"")  + "['" + tree.divThing.sid + "'] = " + tree.divThing.sid));
		}
		for (Entry<String, String> sa : tree.divThing.staticAttrs.entrySet())
			ir.add(JSForm.flex(tree.divThing.id+".setAttribute('" + sa.getKey() +"', '" + sa.getValue() +"')"));
		if (tree.containsThing == AbstractTreeNode.LIST) {
			ir.add(JSForm.flex("wrapper.infoAbout['" + tree.divThing.id + "'] = {}"));
			ir.add(JSForm.flex("// TODO: insert any current contents of the CROSET using insertItem").noSemi());
			ir.add(JSForm.flex("this._" + tree.divThing.id + "_formatList(doc, wrapper)"));
		}
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
//		if (tree.divThing.complexAttrs)
//			ir.add(JSForm.flex("wrapper.infoAbout['" + struct + "']" + (inList?"[item.id]":"")  + "['" + tree.divThing.sid +"'] = " + tree.divThing.sid));
		for (Entry<String, HSIEForm> eh : tree.divThing.handlers.entrySet()) {
			JSForm.assign(ir, "var eh", eh.getValue());
			JSForm cev = JSForm.flex(tree.divThing.id + "['on" + eh.getKey() + "'] = function(event)").needBlock();
			cev.add(JSForm.flex("wrapper.dispatchEvent(event, eh)"));
			ir.add(cev);
		}
		for (VisualTree t : tree.children)
			generateVisualTree(ir, tree.divThing.id, false, inList, t);
	}

	protected void createSomething(JSForm ir, Base c) {
		if (c instanceof Content) {
			JSForm invoke = JSForm.flex("card._" + c.id +"(doc, wrapper)");
			ir.add(invoke);
		} else if (c instanceof ULList) {
			JSForm invoke = JSForm.flex("card._" + c.id +"_formatList(doc, wrapper)");
			ir.add(invoke);
		} else if (c instanceof Struct) {
			JSForm invoke = JSForm.flex("card._" + c.id +"(doc, wrapper, parent)"); // I think "parent" here should be a variable
			ir.add(invoke);
		} else if (!(c instanceof Block))
			throw new UtilException("not handled: " + c.getClass());
	}

	private void genFunction(TemplateAbstractModel tam, Base c) {
		List<String> args;
		String suffix = "";
		if (c instanceof Struct)
			args = CollectionUtils.listOf("doc", "wrapper", "parent");
		else if (c instanceof Content)
			args = CollectionUtils.listOf("doc", "wrapper");
		else if (c instanceof ULList) {
			args = CollectionUtils.listOf("doc", "wrapper", "item", "before");
			suffix = "_itemInserted";
		} else if (c instanceof Block) {
			return;
		} else
			throw new UtilException("Can't generate " + c.getClass());
		JSForm ff = JSForm.flexFn(tam.prefix + ".prototype._" + c.id + suffix, args);
		target.add(ff);
		if (c instanceof Struct) {
			ff.add(JSForm.flex("wrapper.infoAbout['" + c.id + "'] = {}"));
//			int vidx = 1;
			Struct struct = (Struct)c;
			templateChildren(ff, struct.id, false, struct.children);
		} else if (c instanceof Content) {
			Content cc = (Content) c;
			ff.add(JSForm.flex("var span = doc.getElementById(wrapper.infoAbout['" + cc.struct +"']['" + cc.sid + "'])"));
			ff.add(JSForm.flex("span.innerHTML = ''"));
			JSForm.assign(ff, "var textContent", cc.expr);
			ff.add(JSForm.flex("var text = doc.createTextNode(textContent)"));
			ff.add(JSForm.flex("span.appendChild(text)"));
		} else if (c instanceof ULList) {
			ULList l = (ULList) c;
			ff.add(JSForm.flex("var parent = doc.getElementById(wrapper.infoAbout['" + l.struct + "']['" + l.id + "'])"));
			ff.add(JSForm.flex("wrapper.infoAbout['" + l.id + "'][item.id] = { item: item }"));
			boolean hasComplex = templateChildren(ff, l.id, true, l.children);
			if (hasComplex) {
				ff.add(JSForm.flex("this._" + l.id + "_formatItem(doc, wrapper, wrapper.infoAbout['" + l.id + "'][item.id])"));
			}
			JSForm ic = JSForm.flexFn(tam.prefix + ".prototype._" + c.id + "_itemChanged", CollectionUtils.listOf("doc", "wrapper", "item"));
			for (Base x : l.children)
				genFunction(tam, x);
			target.add(ic);
			JSForm fi = JSForm.flexFn(tam.prefix + ".prototype._" + c.id + "_formatItem", CollectionUtils.listOf("doc", "wrapper", "item"));
			target.add(fi);
			JSForm fl = JSForm.flexFn(tam.prefix + ".prototype._" + c.id + "_formatList", CollectionUtils.listOf("doc", "wrapper"));
			target.add(fl);
		} else
			throw new UtilException("not handled: " + c.getClass());
	}

	protected boolean templateChildren(JSForm ff, String struct, boolean inList, List<Base> children) {
		boolean hasComplex = false;
		for (Base b : children) {
			if (b instanceof Content) {
				Content cc = (Content) b;
				ff.add(JSForm.flex("var " + cc.sid + " = wrapper.nextSlotId()"));
				ff.add(JSForm.flex("var " + cc.span + " = doc.createElement('span')"));
				ff.add(JSForm.flex(cc.span + ".setAttribute('id', " + cc.sid + ")"));
				ff.add(JSForm.flex(cc.parent + ".appendChild(" + cc.span + ")"));
				ff.add(JSForm.flex("wrapper.infoAbout['" + struct + "']" + (inList?"[item.id]":"")  + "['" + cc.sid +"'] = " + cc.sid));
			} else if (b instanceof Block) {
				Block bb = (Block) b;
				hasComplex |= bb.complexAttrs != null;
				ff.add(JSForm.flex("var " + b.id + " = doc.createElement('" + bb.tag + "')"));
				if (bb.sid != null) {
					ff.add(JSForm.flex("var " + bb.sid + " = wrapper.nextSlotId()"));
					ff.add(JSForm.flex(bb.id + ".setAttribute('id', " + bb.sid + ")"));
				}
				for (Entry<String, String> sa : bb.staticAttrs.entrySet())
					ff.add(JSForm.flex(bb.id+".setAttribute('" + sa.getKey() +"', '" + sa.getValue() +"')"));
				JSForm ip = ff;
				if (inList) {
					JSForm ie = JSForm.flex("if (before)").needBlock();
					ff.add(ie);
					ie.add(JSForm.flex(bb.parent + ".insertBefore(" + bb.id + ", before)"));
					JSForm es = JSForm.flex("else").needBlock();
					ff.add(es);
					ip = es;
				}
				ip.add(JSForm.flex(bb.parent + ".appendChild(" + bb.id + ")"));
				if (bb.complexAttrs != null)
					ff.add(JSForm.flex("wrapper.infoAbout['" + struct + "']" + (inList?"[item.id]":"")  + "['" + bb.sid +"'] = " + bb.sid));
				for (Entry<String, HSIEForm> eh : bb.handlers.entrySet()) {
					JSForm.assign(ff, "var eh", eh.getValue());
					JSForm cev = JSForm.flex(bb.id + "['on" + eh.getKey() + "'] = function(event)").needBlock();
					cev.add(JSForm.flex("wrapper.dispatchEvent(event, eh)"));
					ff.add(cev);
				}
			} else if (b instanceof ULList) {
				ULList ul = (ULList) b;
				ff.add(JSForm.flex("var " + b.id + " = doc.createElement('" + ul.tag + "')"));
				ff.add(JSForm.flex("var " + ul.sid + " = wrapper.nextSlotId()"));
				ff.add(JSForm.flex(b.id + ".setAttribute('id', " + ul.sid + ")"));
				ff.add(JSForm.flex("wrapper.infoAbout['" + struct + "']['" + ul.id + "'] = " + ul.sid));
				for (Entry<String, String> sa : ul.staticAttrs.entrySet())
					ff.add(JSForm.flex(ul.id+".setAttribute('" + sa.getKey() +"', '" + sa.getValue() +"')"));
				ff.add(JSForm.flex(ul.parent + ".appendChild(" + ul.id + ")"));
			} else
				throw new UtilException("not handled: " + b.getClass());
		}
		
		return hasComplex;
	}

	public JSForm generateTemplateTree(String name, String templateName) {
		JSForm ret = new JSForm(name + "." + templateName + " =").needBlock().needSemi();
		target.add(ret);
		return ret;
	}

	public void generateTree(JSForm block, Element ret) {
		StringBuilder thisOne = new StringBuilder("type: '" + ret.type + "', ");
		if (ret.fn != null) { 
			int idx = ret.fn.lastIndexOf(".");
			thisOne.append("fn: " + ret.fn.substring(0, idx+1) + "prototype" + ret.fn.substring(idx) + ", ");
		}
		if (ret.var != null) {
			thisOne.append("var: '" + ret.var + "', ");
		}
		if (ret.val != null) {
			int idx = ret.val.lastIndexOf(".");
			thisOne.append("val: " + ret.val.substring(0, idx+1) + "prototype" + ret.val.substring(idx) + ", ");
		}
		thisOne.append("route: '" + ret.route + "'");
		boolean wantChildrenArray = true;
		if (!ret.children.isEmpty()) {
			if ("div".equals(ret.type)) {
				thisOne.append(", children:");
			} else if ("switch".equals(ret.type)) {
				thisOne.append(", cases:");
			} else if ("case".equals(ret.type) || "list".equals(ret.type)) {
				thisOne.append(", template:");
				wantChildrenArray = false;
			} else
				throw new UtilException(ret.type + " cannot have children");
		}
		JSForm next = new JSForm(thisOne.toString());
		next.noSemi();
		block.add(next);

		if (!ret.children.isEmpty()) {
			if (wantChildrenArray) {
				next.nestArray();
				for (Element e : ret.children) {
					JSForm wrapper = new JSForm("").needBlock();
					next.add(wrapper);
					generateTree(wrapper, e);
				}
			} else {
				generateTree(next, ret.children.get(0));
			}
		}
	}

	public JSForm generateUpdateTree(String prefix) {
		JSForm ret = new JSForm(prefix + ".updates =").needBlock().needSemi();
		target.add(ret);
		return ret;
	}

	public void generateUpdates(JSForm block, String prefix, ListMap<String, Update> updates) {
		JSForm prev = null;
		for (String s : updates.keySet()) {
			if (prev != null)
				prev.comma();
			JSForm f = new JSForm(s +":").nestArray().noSemi();
			block.add(f);
			for (Update u : updates.get(s)) {
				JSForm g = new JSForm("").needBlock().noSemi();
				g.add(new JSForm("route: '" + u.routeChanges.name() + "', node: " + u.routeChanges.path(new StringBuilder(prefix+".template")) + ", action: '" + u.updateType + "'" + (u.list != null?", list: '" + u.list +"'":"")).noSemi());
				f.add(g);
			}
			prev = f;
		}
	}

}
