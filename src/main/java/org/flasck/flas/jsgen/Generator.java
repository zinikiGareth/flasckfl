package org.flasck.flas.jsgen;

import java.util.ArrayList;
import java.util.List;

import org.flasck.flas.Rewriter;
import org.flasck.flas.hsie.HSIE;
import org.flasck.flas.jsform.JSForm;
import org.flasck.flas.parsedForm.CardDefinition;
import org.flasck.flas.parsedForm.ContractImplements;
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

public class Generator {

	public JSForm generate(HSIEForm input) {
		JSForm ret = JSForm.function(input.fnName, input.vars, input.alreadyUsed, input.nformal);
		generateBlock(input.fnName, input, ret, input);
		return ret;
	}

	public JSForm generate(String name, StructDefn sd) {
		JSForm ret = JSForm.function(name, CollectionUtils.listOf(new Var(0)), 0, 1);
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
					HSIEForm form = HSIE.handleExpr(x.init);
					form.dump();
					generateField(defass, x, form);
					generateField(elseBlock, x, form);
				}
			}
		}
		return ret;
	}

	private void generateField(JSForm defass, StructField x, HSIEForm form) {
		if (form == null)
			defass.add(new JSForm("this."+ x.name + " = undefined"));
		else
			JSForm.assign(defass, "this." + x.name, form);
	}

	public JSForm generate(String name, CardDefinition card) {
		JSForm cf = JSForm.function(name, CollectionUtils.listOf(new Var(0)), 0, 1);
		cf.add(new JSForm("var _self = this"));
		cf.add(new JSForm("this._ctor = '" + name + "'"));
		cf.add(new JSForm("this._wrapper = v0.wrapper"));
		cf.add(new JSForm("this._special = 'card'"));
		if (card.state != null) {
			for (StructField fd : card.state.fields) {
				HSIEForm form = null;
				if (fd.init != null) {
					form = HSIE.handleExpr(fd.init);
					form.dump();
				}

				generateField(cf, fd, form);
			}
		}
		cf.add(new JSForm("this.contracts = {}"));
		int pos = 0;
		for (ContractImplements ci : card.contracts) {
			cf.add(new JSForm("this.contracts['" + ci.type +"'] = new "+ name +"._C" +pos + "(this)"));
			if (ci.referAsVar != null)
				cf.add(new JSForm("this." + ci.referAsVar + " = this.contracts['" + ci.type + "']"));
			pos++;
		}
		pos = 0;
		for (HandlerImplements hi : card.handlers) {
			List<String> tmp = new ArrayList<String>();
			for (int i=0;i<hi.boundVars.size();i++)
				tmp.add("v" +i);
			JSForm ctor = new JSForm("this."+Rewriter.basename(hi.type) + " = function(" + String.join(",",tmp) + ")").strict();
			cf.add(ctor);
			tmp.add(0, "_self");
			ctor.add(new JSForm("return new "+name +"._H" +pos+"(" + String.join(",", tmp) +")"));
			pos++;
		}
		return cf;
	}

	public JSForm generateContract(String name, ContractImplements ci, int pos) {
		String myname = name +"._C"+pos;
		JSForm ret = JSForm.function(name, CollectionUtils.listOf(new Var(0)), 0, 1);
		ret.add(new JSForm("this._ctor = '" + myname + "'"));
		ret.add(new JSForm("this._card = v0"));
		ret.add(new JSForm("this._special = 'contract'"));
		ret.add(new JSForm("this._contract = '" + ci.type + "'"));
		ret.add(new JSForm("this._onchan = null"));
		return ret;
	}

	public JSForm generateHandler(String name, HandlerImplements hi, int pos) {
		String myname = name +"._H"+pos;
		List<Var> vars = new ArrayList<Var>();
		vars.add(null);
		for (int i=1;i<=hi.boundVars.size();i++)
			vars.add(new Var(i));
		JSForm ret = JSForm.function(myname, vars, 0, hi.boundVars.size() + 1);
		ret.add(new JSForm("this._ctor = '" + myname + "'"));
		ret.add(new JSForm("this._card = v0"));
		ret.add(new JSForm("this._special = 'handler'"));
		ret.add(new JSForm("this._contract = '" + hi.type + "'"));
		ret.add(new JSForm("this._onchan = null"));
		int v = 1;
		for (String s : hi.boundVars) 
			ret.add(new JSForm("this." + s + " = v" + v++));
		return ret;
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
		render.add(new JSForm("myblock.appendChild(doc.createTextNode(this." + ((TemplateToken)tl.contents.get(0)).text + "))"));
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
			} else
				h.dumpOne(0);
		}
	}
}
