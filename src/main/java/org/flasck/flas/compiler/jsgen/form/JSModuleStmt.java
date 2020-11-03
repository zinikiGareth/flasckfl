package org.flasck.flas.compiler.jsgen.form;

import org.flasck.flas.compiler.jsgen.creators.JVMCreationContext;
import org.flasck.jvm.J;
import org.zinutils.bytecode.IExpr;
import org.zinutils.bytecode.NewMethodDefiner;
import org.zinutils.bytecode.mock.IndentWriter;

public class JSModuleStmt implements IVForm {
	private final JSExpr obj;
	private final String javaIF;
	private final String javaModule;

	public JSModuleStmt(JSExpr obj, String javaIF, String javaModule) {
		this.obj = obj;
		this.javaIF = javaIF;
		this.javaModule = javaModule;
	}

	@Override
	public void write(IndentWriter w) {
		if (obj != null) {
			w.print(obj.asVar());
			w.print(".");
		}
		w.print("module('");
		w.print(javaModule);
		w.print("')");
	}

	@Override
	public void generate(JVMCreationContext jvm) {
		NewMethodDefiner md = jvm.method();
		IExpr mod = md.castTo(md.callInterface(J.OBJECT, jvm.argAsIs(obj), "module", md.classConst(javaModule)), javaIF);
		jvm.local(this, mod);
	}

	@Override
	public void asivm(IVFWriter iw) {
		iw.println("module " + javaModule);
	}
	
	@Override
	public String asVar() {
		// TODO Auto-generated method stub
		return null;
	}
}
