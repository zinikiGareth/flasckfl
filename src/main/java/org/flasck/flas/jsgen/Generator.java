package org.flasck.flas.jsgen;

import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;

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
import org.flasck.flas.parsedForm.TemplateLine;
import org.flasck.flas.tokenizers.TemplateToken;
import org.flasck.flas.vcode.hsieForm.BindCmd;
import org.flasck.flas.vcode.hsieForm.ErrorCmd;
import org.flasck.flas.vcode.hsieForm.HSIEBlock;
import org.flasck.flas.vcode.hsieForm.HSIEForm;
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
			idx = jsname.lastIndexOf("._C");
			if (idx == -1) idx = jsname.lastIndexOf("._H");
			if (idx == -1) idx = jsname.lastIndexOf("._S");
			if (idx != -1) jsname = jsname.substring(0, idx+1) + "_" + jsname.substring(idx+1);
		}
		JSForm ret = JSForm.function(jsname, input.vars, input.alreadyUsed, input.nformal);
		generateBlock(input.fnName, input, ret, input);
		target.add(ret);
	}

	public void generate(String name, StructDefn sd) {
		if (!sd.generate)
			return;
		JSForm ret = JSForm.function(name, CollectionUtils.listOf(new Var(0)), 0, 1);
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
		String clzname = ctorName.replace("._H", ".__H");
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

	/* We want something like this:
test.ziniki.CounterCard.prototype._templateLine1 = {
	tag: 'span',
	render: function(doc, myblock) {
		myblock.appendChild(doc.createTextNode(this.counter));
	}
}
	 */
	public JSForm generateTemplateLine(TemplateRenderState trs, TemplateLine tl) {
		JSForm ret = new JSForm(trs.name + ".prototype._templateLine"+trs.lineNo() + " =").needBlock();
		ret.add(new JSForm("tag: 'span'").comma());
		JSForm render = new JSForm("render: function(doc, myblock)").strict();
		ret.add(render);
		for (Object o : tl.contents) {
			if (o instanceof TemplateToken) {
				TemplateToken tt = (TemplateToken) o;
				if (tt.type == TemplateToken.IDENTIFIER)
					render.add(new JSForm("myblock.appendChild(doc.createTextNode(this." + tt.text + "))"));
				else
					throw new UtilException("Cannot handle " + tt.type);
			} else
				throw new UtilException("Cannot handle " + o.getClass());
		}
		return ret;
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

	public JSForm generateTemplateTree(String name, String templateName) {
		JSForm ret = new JSForm(name + "." + templateName + " =").needBlock().needSemi();
		target.add(ret);
		return ret;
	}

	public void generateTree(JSForm block, Element ret) {
		int idx = ret.fn.lastIndexOf(".");
		String jsname = ret.fn.substring(0, idx+1) + "prototype" + ret.fn.substring(idx);

		StringBuilder thisOne = new StringBuilder("type: '" + ret.type + "', fn: " + jsname + ", route: '" +ret.route + "'"); //lass: [");
//		String sep = "";
//		for (String s : ret.classes) {
//			thisOne.append(sep);
//			thisOne.append("'");
//			thisOne.append(s);
//			thisOne.append("'");
//			sep = ", ";
//		}
//		for (String ae : ret.clsexprs) {
//			thisOne.append(sep);
//			int idx2 = ae.lastIndexOf(".");
//			thisOne.append(ae.substring(0, idx2+1) + "prototype" + ae.substring(idx2));
//			sep = ", ";
//		}
//		thisOne.append("]");
		if (!ret.children.isEmpty())
			thisOne.append(", children:");
		JSForm next = new JSForm(thisOne.toString());
		next.noSemi();
		block.add(next);

		if (!ret.children.isEmpty()) {
			next.nestArray();
			for (Element e : ret.children) {
				JSForm wrapper = new JSForm("").needBlock();
				next.add(wrapper);
				generateTree(wrapper, e);
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
				g.add(new JSForm("route: '" + u.routeChanges + "', node: " + nodepath(new StringBuilder(prefix+".template"), u.routeChanges) + ", action: '" + u.updateType + "'").noSemi());
				f.add(g);
			}
			prev = f;
		}
	}

	private String nodepath(StringBuilder sb, String route) {
		int idx = route.indexOf(".");
		if (idx == -1) {
			if (route.length() > 0)
				sb.append(".children[" + route + "]");
			return sb.toString();
		} else {
			sb.append(".children[" + route.substring(0, idx) + "]");
			return nodepath(sb, route.substring(idx+1));
		}
	}

}
