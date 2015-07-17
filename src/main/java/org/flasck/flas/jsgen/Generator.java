package org.flasck.flas.jsgen;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.flasck.flas.TemplateAbstractModel;
import org.flasck.flas.TemplateAbstractModel.Base;
import org.flasck.flas.TemplateAbstractModel.Content;
import org.flasck.flas.TemplateAbstractModel.Struct;
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
					HSIEForm form = new HSIE(errors).handleExpr(x.init);
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
				form = new HSIE(errors).handleExpr(x.getValue());
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

	public void generate(TemplateAbstractModel tam) {
		// Firstly firstly, create the "initialRender" function
		JSForm ir = JSForm.flexFn(tam.prefix + ".initialRender", CollectionUtils.listOf("doc", "wrapper", "parent", "card"));
		// first spit out struct items
		for (Base c : tam.contents) {
			if (c instanceof Struct) {
				JSForm invoke = JSForm.flex("card._" + c.id +"(doc, wrapper, parent)"); // I think "parent" here should be a variable
				ir.add(invoke);
			}
		}
		// now do everything else
		for (Base c : tam.contents) {
			if (c instanceof Content) {
				JSForm invoke = JSForm.flex("card._" + c.id +"(doc, wrapper)");
				ir.add(invoke);
			} else if (!(c instanceof Struct))
				throw new UtilException("not handled: " + c.getClass());
		}
		target.add(ir);
		
		// Now do the actual functions 
		for (Base c : tam.contents) {
			List<String> args;
			if (c instanceof Struct)
				args = CollectionUtils.listOf("doc", "wrapper", "parent");
			else if (c instanceof Content)
				args = CollectionUtils.listOf("doc", "wrapper");
			else
				throw new UtilException("Can't generate " + c.getClass());
			JSForm ff = JSForm.flexFn(tam.prefix + ".prototype._" + c.id, args);
			if (c instanceof Struct) {
				ff.add(JSForm.flex("wrapper.infoAbout['" + c.id + "'] = {}"));
//				int vidx = 1;
				for (Base b : ((Struct)c).children) {
					if (b instanceof Content) {
						Content cc = (Content) b;
//						String sid = "sid" + (vidx++);
//						String span = "span" + (vidx++);
						ff.add(JSForm.flex("var " + cc.sid + " = wrapper.nextSlotId()"));
						ff.add(JSForm.flex("var " + cc.span + " = doc.createElement('span')"));
						ff.add(JSForm.flex(cc.span + ".setAttribute('id', " + cc.sid + ")"));
						ff.add(JSForm.flex("parent.appendChild(" + cc.span + ")"));
						ff.add(JSForm.flex("wrapper.infoAbout['" + c.id + "']['" + cc.sid +"'] = " + cc.sid));
					} else
						throw new UtilException("not handled: " + c.getClass());
				}
			} else if (c instanceof Content) {
				Content cc = (Content) c;
				ff.add(JSForm.flex("var span = doc.getElementById(wrapper.infoAbout['" + cc.struct +"']['" + cc.sid + "'])"));
				ff.add(JSForm.flex("span.innerHTML = ''"));
				cc.expr.dump();
				JSForm.assign(ff, "var textContent", cc.expr);
				ff.add(JSForm.flex("var text = doc.createTextNode(textContent)"));
				ff.add(JSForm.flex("span.appendChild(text)"));
			} else
				throw new UtilException("not handled: " + c.getClass());
			target.add(ff);
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
