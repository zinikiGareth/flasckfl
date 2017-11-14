package org.flasck.flas.jsgen;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.TreeSet;

import org.flasck.flas.compiler.HSIEFormGenerator;
import org.flasck.flas.jsform.JSForm;
import org.flasck.flas.jsform.JSTarget;
import org.flasck.flas.rewriter.RepoVisitor;
import org.flasck.flas.rewrittenForm.CardGrouping;
import org.flasck.flas.rewrittenForm.CardGrouping.ContractGrouping;
import org.flasck.flas.rewrittenForm.CardGrouping.ServiceGrouping;
import org.flasck.flas.rewrittenForm.HandlerLambda;
import org.flasck.flas.rewrittenForm.RWContractDecl;
import org.flasck.flas.rewrittenForm.RWContractImplements;
import org.flasck.flas.rewrittenForm.RWContractService;
import org.flasck.flas.rewrittenForm.RWHandlerImplements;
import org.flasck.flas.rewrittenForm.RWObjectDefn;
import org.flasck.flas.rewrittenForm.RWStructDefn;
import org.flasck.flas.rewrittenForm.RWStructField;
import org.flasck.flas.rewrittenForm.ScopedVar;
import org.flasck.flas.template.TemplateGenerator;
import org.flasck.flas.vcode.hsieForm.HSIEForm;
import org.flasck.flas.vcode.hsieForm.Var;

public class Generator implements RepoVisitor, HSIEFormGenerator {
	private final JSTarget target;

	public Generator(JSTarget target) {
		this.target = target;
	}
	
	@Override
	public TemplateGenerator templateGenerator() {
		return new JSTemplateGenerator(target);
	}

	@Override
	public void visitStructDefn(RWStructDefn sd) {
		if (!sd.generate)
			return;
		int idx = sd.name().lastIndexOf(".");
		String uname = sd.name().substring(0, idx+1) + "_" + sd.name().substring(idx+1);
		JSForm ret = JSForm.function(uname, Arrays.asList(new Var(0)), new TreeSet<ScopedVar>(), 1);
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
					defass.add(JSForm.flex("this." + x.name+ " = FLEval.full(" + x.init.jsName() + "())"));
					elseBlock.add(JSForm.flex("this." + x.name+ " = FLEval.full(" + x.init.jsName() + "())"));
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
		JSForm ctor = JSForm.function(sd.name(), vars, new TreeSet<ScopedVar>(), vars.size());
		ctor.add(new JSForm("return new " + uname + "({" + String.join(", ", fields) + "})"));
		target.add(ctor);
	}

	@Override
	public void visitObjectDefn(RWObjectDefn od) {
		if (!od.generate)
			return;
		JSForm ret = JSForm.function(od.myName().jsUName(), Arrays.asList(new Var(0)), new TreeSet<>(), 0);
		target.add(ret);
	}

	private void generateField(JSForm defass, String field, String tfn) {
		if (tfn == null)
			defass.add(new JSForm("this."+ field + " = undefined"));
		else
			defass.add(JSForm.flex("this." + field + " = FLEval.full(" + tfn + "())"));
	}

	@Override
	public void visitContractDecl(RWContractDecl cd) {
	}

	public void visitCardGrouping(CardGrouping card) {
		String name = card.getName().jsName();
		String lname = card.getName().jsUName();
		JSForm cf = JSForm.function(lname, Arrays.asList(new Var(0)), new TreeSet<ScopedVar>(), 1);
		cf.add(new JSForm("var _self = this"));
		cf.add(new JSForm("this._ctor = '" + name + "'"));
		cf.add(new JSForm("this._wrapper = v0.wrapper"));
		cf.add(new JSForm("this._special = 'card'"));
		for (RWStructField x : card.struct.fields) {
			if (x.init != null)
				generateField(cf, x.name, x.init.jsName());
		}
		cf.add(new JSForm("this._services = {}"));
		for (ServiceGrouping cs : card.services) {
			cf.add(new JSForm("this._services['" + cs.type + "'] = " + cs.implName.jsName() + ".apply(this)"));
			if (cs.referAsVar != null)
				cf.add(new JSForm("this." + cs.referAsVar + " = this._services['" + cs.type + "']"));
		}
		cf.add(new JSForm("this._contracts = {}"));
		for (ContractGrouping ci : card.contracts) {
			cf.add(new JSForm("this._contracts['" + ci.contractName.uniqueName() +"'] = "+ ci.implName.jsName() + ".apply(this)"));
			if (ci.referAsVar != null)
				cf.add(new JSForm("this." + ci.referAsVar + " = this._contracts['" + ci.contractName.uniqueName() + "']"));
		}
		target.add(cf);
		JSForm ci = JSForm.function(name, Arrays.asList(new Var(0)), new TreeSet<ScopedVar>(), 1);
		ci.add(new JSForm("return new " + lname + "(v0)"));
		target.add(ci);
	}

	@Override
	public void visitContractImpl(RWContractImplements ci) {
		String ctorName = ci.realName.jsName();
		String clzname = ctorName.replace("._C", ".__C");
		JSForm clz = JSForm.function(clzname, Arrays.asList(new Var(0)), new TreeSet<ScopedVar>(), 1);
		clz.add(new JSForm("this._ctor = '" + ctorName + "'"));
		clz.add(new JSForm("this._card = v0"));
		clz.add(new JSForm("this._special = 'contract'"));
		clz.add(new JSForm("this._contract = '" + ci.name() + "'"));
		target.add(clz);

		JSForm ctor = JSForm.function(ctorName, new ArrayList<Var>(), new TreeSet<ScopedVar>(), 0);
		ctor.add(new JSForm("return new " + clzname + "(this)"));
		target.add(ctor);
	}

	public void visitServiceImpl(RWContractService cs) {
		String ctorName = cs.realName.uniqueName();
		String clzname = ctorName.replace("._S", ".__S");
		JSForm clz = JSForm.function(clzname, Arrays.asList(new Var(0)), new TreeSet<ScopedVar>(), 1);
		clz.add(new JSForm("this._ctor = '" + ctorName + "'"));
		clz.add(new JSForm("this._card = v0"));
		clz.add(new JSForm("this._special = 'service'"));
		clz.add(new JSForm("this._contract = '" + cs.name() + "'"));
		target.add(clz);

		JSForm ctor = JSForm.function(ctorName, new ArrayList<Var>(), new TreeSet<ScopedVar>(), 0);
		ctor.add(new JSForm("return new " + clzname + "(this)"));
		target.add(ctor);
	}

	public void visitHandlerImpl(RWHandlerImplements hi) {
		String ctorName = hi.handlerName.uniqueName();
		target.ensurePackagesFor(ctorName);
		String clzname = hi.handlerName.jsUName();
		List<Var> vars = new ArrayList<Var>();
		for (int i=0;i<=hi.boundVars.size();i++)
			vars.add(new Var(i));
		
		int v = hi.inCard?1:0;
		JSForm clz = JSForm.function(clzname, vars, new TreeSet<ScopedVar>(), hi.boundVars.size() + v);
		clz.add(new JSForm("this._ctor = '" + ctorName + "'"));
		if (hi.inCard)
			clz.add(new JSForm("this._card = v0"));
		clz.add(new JSForm("this._special = 'handler'"));
		clz.add(new JSForm("this._contract = '" + hi.name() + "'"));
		for (HandlerLambda s : hi.boundVars) 
			clz.add(new JSForm("this." + s.var + " = v" + v++));
		target.add(clz);

		JSForm ctor = JSForm.function(ctorName, vars, new TreeSet<ScopedVar>(), hi.boundVars.size());
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

	@Override
	public void generate(HSIEForm form) {
		new JSFormGenerator(target, form).generate();
	}
}
