package org.flasck.flas.compiler.jsgen.form;

import java.util.ArrayList;
import java.util.List;

import org.flasck.flas.compiler.jsgen.creators.JVMCreationContext;
import org.flasck.jvm.J;
import org.zinutils.bytecode.IExpr;
import org.zinutils.bytecode.NewMethodDefiner;
import org.zinutils.bytecode.mock.IndentWriter;

public class JSModuleStmt implements IVForm {
	private final JSExpr obj;
	private final String javaModule;
	private final String meth;
	private final JSExpr[] args;

	public JSModuleStmt(JSExpr obj, String javaModule, String method, JSExpr... args) {
		this.obj = obj;
		this.javaModule = javaModule;
		this.meth = method;
		this.args = args;
	}

	@Override
	public void write(IndentWriter w) {
		if (obj != null) {
			w.print(obj.asVar());
			w.print(".");
		}
		w.print("module('");
		w.print(javaModule);
		w.print("').");
		w.print(meth);
		w.print("(_cxt");
		for (JSExpr e : args) {
			w.print(", ");
			w.print(e.asVar());
		}
		w.println(");");
	}

	@Override
	public void generate(JVMCreationContext jvm) {
 		List<IExpr> as = new ArrayList<>();
 		as.add(jvm.cxt());
		for (JSExpr e : args) {
			as.add(jvm.arg(e));
		}
		NewMethodDefiner md = jvm.method();
		IExpr mod = md.castTo(md.callInterface(J.OBJECT, jvm.argAsIs(obj), "module", md.classConst(javaModule)), javaModule);
		IExpr ret = md.callVirtual("void", mod, meth, as.toArray(new IExpr[as.size()]));
		jvm.local(this, ret);
	}

	@Override
	public void asivm(IVFWriter iw) {
		iw.println("module " + javaModule + " " + meth);
	}
	
	@Override
	public String asVar() {
		// TODO Auto-generated method stub
		return null;
	}
}
