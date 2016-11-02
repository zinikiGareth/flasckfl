package org.flasck.flas.jsgen;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;

import org.flasck.flas.jsform.JSForm;
import org.flasck.flas.jsform.JSTarget;
import org.flasck.flas.rewrittenForm.CardGrouping;
import org.flasck.flas.rewrittenForm.CardGrouping.ContractGrouping;
import org.flasck.flas.rewrittenForm.CardGrouping.ServiceGrouping;
import org.flasck.flas.rewrittenForm.HandlerLambda;
import org.flasck.flas.rewrittenForm.RWContractImplements;
import org.flasck.flas.rewrittenForm.RWContractService;
import org.flasck.flas.rewrittenForm.RWHandlerImplements;
import org.flasck.flas.rewrittenForm.RWStructDefn;
import org.flasck.flas.rewrittenForm.RWStructField;
import org.flasck.flas.rewrittenForm.VarNestedFromOuterFunctionScope;
import org.flasck.flas.vcode.hsieForm.BindCmd;
import org.flasck.flas.vcode.hsieForm.ErrorCmd;
import org.flasck.flas.vcode.hsieForm.HSIEBlock;
import org.flasck.flas.vcode.hsieForm.HSIEForm;
import org.flasck.flas.vcode.hsieForm.HSIEForm.CodeType;
import org.flasck.flas.vcode.hsieForm.Head;
import org.flasck.flas.vcode.hsieForm.IFCmd;
import org.flasck.flas.vcode.hsieForm.PushReturn;
import org.flasck.flas.vcode.hsieForm.Switch;
import org.flasck.flas.vcode.hsieForm.Var;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.zinutils.collections.CollectionUtils;

public class Generator {
	private final JSTarget target;

	public Generator(JSTarget target) {
		this.target = target;
	}
	
	public void generate(HSIEForm input) {
		input.dump(LoggerFactory.getLogger("Generator"));
		String jsname = input.fnName;
		if (input.isMethod()) {
			int idx = jsname.lastIndexOf(".");
			jsname = jsname.substring(0, idx+1) + "prototype" + jsname.substring(idx);
			if (input.mytype == CodeType.HANDLER) {
				idx = jsname.lastIndexOf('.', idx-1);
			} else {
				idx = jsname.lastIndexOf("._C");
				if (idx == -1) idx = jsname.lastIndexOf("._S");
			}
			if (idx != -1) jsname = jsname.substring(0, idx+1) + "_" + jsname.substring(idx+1);
		}
		JSForm ret = JSForm.function(jsname, input.vars, input.scoped, input.nformal);
		generateBlock(input.fnName, input, ret, input);
		target.add(ret);
	}

	public void generate(RWStructDefn sd, Map<String, String> initMap) {
		if (!sd.generate)
			return;
		int idx = sd.name().lastIndexOf(".");
		String uname = sd.name().substring(0, idx+1) + "_" + sd.name().substring(idx+1);
		JSForm ret = JSForm.function(uname, CollectionUtils.listOf(new Var(0)), new TreeSet<VarNestedFromOuterFunctionScope>(), 1);
		ret.add(new JSForm("this._ctor = '" + sd.name() + "'"));
		if (!sd.fields.isEmpty()) {
			JSForm ifBlock = new JSForm("if (v0)");
			ret.add(ifBlock);
			JSForm elseBlock = new JSForm("else").needBlock();
			ret.add(elseBlock);
			for (RWStructField x : sd.fields) {
				JSForm assign = new JSForm("if (v0."+x.name+")");
				assign.add(new JSForm("this."+x.name+" = v0."+x.name));
				ifBlock.add(assign);
				if (x.init != null) {
					JSForm defass = new JSForm("else");
					ifBlock.add(defass);
					defass.add(JSForm.flex("this." + x.name+ " = FLEval.full(" + initMap.get(x.name) + "())"));
					elseBlock.add(JSForm.flex("this." + x.name+ " = FLEval.full(" + initMap.get(x.name) + "())"));
				}
			}
		}
		target.add(ret);

		List<Var> vars = new ArrayList<Var>();
		List<String> fields = new ArrayList<String>();
		int vi = 0;
		for (RWStructField sf : sd.fields) {
			Var v = new Var(vi++);
			vars.add(v);
			fields.add(sf.name+": "+ v);
		}
		JSForm ctor = JSForm.function(sd.name(), vars, new TreeSet<VarNestedFromOuterFunctionScope>(), vars.size());
		ctor.add(new JSForm("return new " + uname + "({" + String.join(", ", fields) + "})"));
		target.add(ctor);
	}

	private void generateField(JSForm defass, String field, String tfn) {
		if (tfn == null)
			defass.add(new JSForm("this."+ field + " = undefined"));
		else
			defass.add(JSForm.flex("this." + field + " = FLEval.full(" + tfn + "())"));
	}

	public void generate(String name, CardGrouping card, Map<String, String> fieldInits) {
		String lname = lname(name, false);
		JSForm cf = JSForm.function(lname, CollectionUtils.listOf(new Var(0)), new TreeSet<VarNestedFromOuterFunctionScope>(), 1);
		cf.add(new JSForm("var _self = this"));
		cf.add(new JSForm("this._ctor = '" + name + "'"));
		cf.add(new JSForm("this._wrapper = v0.wrapper"));
		cf.add(new JSForm("this._special = 'card'"));
		for (RWStructField x : card.struct.fields) {
			if (x.init != null)
				generateField(cf, x.name, fieldInits.get(x.name));
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
		JSForm ci = JSForm.function(name, CollectionUtils.listOf(new Var(0)), new TreeSet<VarNestedFromOuterFunctionScope>(), 1);
		ci.add(new JSForm("return new " + lname + "(v0)"));
		target.add(ci);
	}

	public void generateContract(String ctorName, RWContractImplements ci) {
		String clzname = ctorName.replace("._C", ".__C");
		JSForm clz = JSForm.function(clzname, CollectionUtils.listOf(new Var(0)), new TreeSet<VarNestedFromOuterFunctionScope>(), 1);
		clz.add(new JSForm("this._ctor = '" + ctorName + "'"));
		clz.add(new JSForm("this._card = v0"));
		clz.add(new JSForm("this._special = 'contract'"));
		clz.add(new JSForm("this._contract = '" + ci.name() + "'"));
		target.add(clz);

		JSForm ctor = JSForm.function(ctorName, new ArrayList<Var>(), new TreeSet<VarNestedFromOuterFunctionScope>(), 0);
		ctor.add(new JSForm("return new " + clzname + "(this)"));
		target.add(ctor);
	}

	public void generateService(String ctorName, RWContractService cs) {
		String clzname = ctorName.replace("._S", ".__S");
		JSForm clz = JSForm.function(clzname, CollectionUtils.listOf(new Var(0)), new TreeSet<VarNestedFromOuterFunctionScope>(), 1);
		clz.add(new JSForm("this._ctor = '" + ctorName + "'"));
		clz.add(new JSForm("this._card = v0"));
		clz.add(new JSForm("this._special = 'service'"));
		clz.add(new JSForm("this._contract = '" + cs.name() + "'"));
		target.add(clz);

		JSForm ctor = JSForm.function(ctorName, new ArrayList<Var>(), new TreeSet<VarNestedFromOuterFunctionScope>(), 0);
		ctor.add(new JSForm("return new " + clzname + "(this)"));
		target.add(ctor);
	}

	public void generateHandler(String ctorName, RWHandlerImplements hi) {
		target.ensurePackagesFor(ctorName);
		String clzname = lname(ctorName, false);
		List<Var> vars = new ArrayList<Var>();
		for (int i=0;i<=hi.boundVars.size();i++)
			vars.add(new Var(i));
		
		int v = hi.inCard?1:0;
		JSForm clz = JSForm.function(clzname, vars, new TreeSet<VarNestedFromOuterFunctionScope>(), hi.boundVars.size() + v);
		clz.add(new JSForm("this._ctor = '" + ctorName + "'"));
		if (hi.inCard)
			clz.add(new JSForm("this._card = v0"));
		clz.add(new JSForm("this._special = 'handler'"));
		clz.add(new JSForm("this._contract = '" + hi.name() + "'"));
		for (HandlerLambda s : hi.boundVars) 
			clz.add(new JSForm("this." + s.var + " = v" + v++));
		target.add(clz);

		JSForm ctor = JSForm.function(ctorName, vars, new TreeSet<VarNestedFromOuterFunctionScope>(), hi.boundVars.size());
		StringBuffer sb = new StringBuffer();
		String sep = "";
		if (hi.inCard) {
			sb.append("this");
			sep = ", ";
		}
		vars.remove(vars.size()-1);
		for (Var vi : vars) {
			sb.append(sep + vi);
			sep = ", ";
		}
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
			} else if (h instanceof PushReturn) {
				PushReturn r = (PushReturn) h;
				into.addAll(JSForm.ret(r, form));
			} else if (h instanceof ErrorCmd) {
				into.add(JSForm.error(fn));
			} else {
				System.out.println("Cannot generate block:");
				h.dumpOne((Logger)null, 0);
			}
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
