package org.flasck.flas.jsgen;

import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;

import org.flasck.flas.TemplateAbstractModel;
import org.flasck.flas.TemplateAbstractModel.AbstractTreeNode;
import org.flasck.flas.TemplateAbstractModel.Handler;
import org.flasck.flas.TemplateAbstractModel.OrCase;
import org.flasck.flas.TemplateAbstractModel.VisualTree;
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
import org.flasck.flas.parsedForm.HandlerLambda;
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
		int idx = sd.name().lastIndexOf(".");
		String uname = sd.name().substring(0, idx+1) + "_" + sd.name().substring(idx+1);
		JSForm ret = JSForm.function(uname, CollectionUtils.listOf(new Var(0)), 0, 1);
		ret.add(new JSForm("this._ctor = '" + sd.name() + "'"));
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
		JSForm ctor = JSForm.function(sd.name(), vars, 0, vars.size());
		ctor.add(new JSForm("return new " + uname + "({" + String.join(", ", fields) + "})"));
		target.add(ctor);
		
		target.add(JSForm.flex("FLEval.registerStruct('" + sd.name() +"', " + sd.name() + ", " + uname + ")"));
	}

	private void generateField(JSForm defass, String field, HSIEForm form) {
		if (form == null)
			defass.add(new JSForm("this."+ field + " = undefined"));
		else
			JSForm.assign(defass, "this." + field, form);
	}

	public void generate(String name, CardGrouping card) {
		String lname = lname(name, false);
		JSForm cf = JSForm.function(lname, CollectionUtils.listOf(new Var(0)), 0, 1);
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
			cf.add(new JSForm("this." + x.getKey() + " = FLEval.full(this." + x.getKey() + ")"));
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
		JSForm ci = JSForm.function(name, CollectionUtils.listOf(new Var(0)), 0, 1);
		ci.add(new JSForm("return new " + lname + "(v0)"));
		target.add(ci);
	}

	public void generateContract(String ctorName, ContractImplements ci) {
		String clzname = ctorName.replace("._C", ".__C");
		JSForm clz = JSForm.function(clzname, CollectionUtils.listOf(new Var(0)), 0, 1);
		clz.add(new JSForm("this._ctor = '" + ctorName + "'"));
		clz.add(new JSForm("this._card = v0"));
		clz.add(new JSForm("this._special = 'contract'"));
		clz.add(new JSForm("this._contract = '" + ci.name() + "'"));
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
		clz.add(new JSForm("this._contract = '" + cs.name() + "'"));
		target.add(clz);

		JSForm ctor = JSForm.function(ctorName, new ArrayList<Var>(), 0, 0);
		ctor.add(new JSForm("return new " + clzname + "(this)"));
		target.add(ctor);
	}

	public void generateHandler(String ctorName, HandlerImplements hi) {
		String clzname = lname(ctorName, false);
		List<Var> vars = new ArrayList<Var>();
		for (int i=0;i<=hi.boundVars.size();i++)
			vars.add(new Var(i));
		JSForm clz = JSForm.function(clzname, vars, 0, hi.boundVars.size() + 1);
		clz.add(new JSForm("this._ctor = '" + ctorName + "'"));
		clz.add(new JSForm("this._card = v0"));
		clz.add(new JSForm("this._special = 'handler'"));
		clz.add(new JSForm("this._contract = '" + hi.name() + "'"));
		int v = 1;
		for (Object s : hi.boundVars) 
			clz.add(new JSForm("this." + ((HandlerLambda)s).var + " = v" + v++));
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
				JSForm ir = JSForm.flexFn(lname(tam.prefix, true) + "_initialRender", CollectionUtils.listOf("doc", "wrapper", "parent"));
				target.add(ir);
				generateVisualTree(ir, "parent", false, null, atn.tree);
				generate(tam, null, atn);
				JSForm ft = JSForm.flexFn(lname(tam.prefix, true) + "_formatTop", CollectionUtils.listOf("doc", "wrapper"));
				target.add(ft);
				generateFormatsFor(ft, atn.tree, "wrapper.infoAbout");
			} else if (atn.type == AbstractTreeNode.LIST) {
				{
					JSForm ra = JSForm.flexFn(lname(tam.prefix, true) + "_" + atn.id + "_assign", CollectionUtils.listOf("doc", "wrapper"));
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
				JSForm ii = JSForm.flexFn(lname(tam.prefix, true) + "_" + atn.id + "_itemInserted", CollectionUtils.listOf("doc", "wrapper", "item", "before"));
				target.add(ii);
				ii.add(JSForm.flex("var parent = doc.getElementById(wrapper.infoAbout['" + atn.sid + "'])"));
				ii.add(JSForm.flex("wrapper.infoAbout['" + atn.id + "'][item.id] = { item: item }"));
				for (VisualTree t : atn.tree.children)
					generateVisualTree(ii, "parent", true, atn.id, t);
				ii.add(JSForm.flex("this._" + atn.id + "_formatItem(doc, wrapper, wrapper.infoAbout['" + atn.id + "'][item.id])"));
				JSForm cl = JSForm.flexFn(lname(tam.prefix, true) + "_" + atn.id + "_clear", CollectionUtils.listOf("doc", "wrapper"));
				target.add(cl);
				cl.add(JSForm.flex("var " + atn.id + " = doc.getElementById(wrapper.infoAbout['" + atn.sid + "'])"));
				cl.add(JSForm.flex(atn.id + ".innerHTML = ''"));
				JSForm ic = JSForm.flexFn(lname(tam.prefix, true) + "_" + atn.id + "_itemChanged", CollectionUtils.listOf("doc", "wrapper", "item"));
				target.add(ic);
				generate(tam, ic, atn);
				JSForm fi = JSForm.flexFn(lname(tam.prefix, true) + "_" + atn.id + "_formatItem", CollectionUtils.listOf("doc", "wrapper", "info"));
				target.add(fi);
				for (VisualTree t : atn.tree.children)
					generateFormatsFor(fi, t, "info");
				JSForm fl = JSForm.flexFn(lname(tam.prefix, true) + "_" + atn.id + "_formatList", CollectionUtils.listOf("doc", "wrapper"));
				target.add(fl);
				JSForm lp = JSForm.flex("for (var x in wrapper.infoAbout['" + atn.id + "'])").needBlock();
				fl.add(lp);
				lp.add(JSForm.flex("this._" + atn.id + "_formatItem(doc, wrapper, wrapper.infoAbout['" + atn.id + "'][x])"));
			} else if (atn.type == AbstractTreeNode.CASES) {
				JSForm sw = JSForm.flexFn(lname(tam.prefix, true) + "_" + atn.id + "_switch", CollectionUtils.listOf("doc", "wrapper"));
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
				generate(tam, null, atn);
			} else if (atn.type == AbstractTreeNode.CONTENT) {
				JSForm cc = function;
				if (cc == null) {
					cc = JSForm.flexFn(lname(tam.prefix, true) + "_" + atn.id, CollectionUtils.listOf("doc", "wrapper"));
					target.add(cc);
				}
				cc.add(JSForm.flex("var span = doc.getElementById(wrapper.infoAbout" + (atn.nestedIn != null && atn.nestedIn.id != null?"['" + atn.nestedIn.id + "'][item.id]":"") + "['" + atn.sid + "'])"));
				cc.add(JSForm.flex("span.innerHTML = ''"));
				JSForm.assign(cc, "var textContent", atn.expr);
				cc.add(JSForm.flex("var text = doc.createTextNode(FLEval.full(textContent))"));
				cc.add(JSForm.flex("span.appendChild(text)"));
				if (atn.editable) {
					JSForm rules = JSForm.flex(lname(tam.prefix, true) + "_" + atn.id + "_rules =").needBlock();
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
					cc = JSForm.flexFn(lname(tam.prefix, true) + "_" + atn.id, CollectionUtils.listOf("doc", "wrapper"));
					target.add(cc);
				}
				cc.add(JSForm.flex("wrapper.showCard('" + atn.sid + "', { card: " + atn.card.explicitCard + "})"));
			} else if (atn.type == AbstractTreeNode.D3) {
				JSForm cc = function;
				if (cc == null) {
					cc = JSForm.flexFn(lname(tam.prefix, true) + "_" + atn.id, CollectionUtils.listOf("doc", "wrapper"));
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
			HSIEForm form = new HSIE(errors).handleExpr(t.divThing.complexFormats, Type.CARD);
			JSForm.assign(fi, "var cls", form);
			fi.add(JSForm.flex("doc.getElementById(" + var + "['" + t.divThing.sid + "']).setAttribute('class', join(FLEval.full(cls), ' '))"));
		}
		for (Entry<String, Object> x : t.divThing.exprAttrs.entrySet()) {
			fi.add(JSForm.flex("var item = " + var + ".item"));
			HSIEForm form = new HSIE(errors).handleExpr(x.getValue(), Type.CARD);
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
			cev.add(JSForm.flex("wrapper.dispatchEvent(event, eh" + eh.id + ")"));
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

	public static String lname(String name, boolean appendProto) {
		int idx = name.lastIndexOf('.');
		String lname = name.substring(0, idx+1) + "_" + name.substring(idx+1);
		if (appendProto)
			return lname + ".prototype.";
		return lname;
	}
}
