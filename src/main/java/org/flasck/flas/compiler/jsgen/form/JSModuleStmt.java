package org.flasck.flas.compiler.jsgen.form;

import org.flasck.flas.compiler.jsgen.creators.JVMCreationContext;
import org.flasck.jvm.J;
import org.zinutils.bytecode.IExpr;
import org.zinutils.bytecode.NewMethodDefiner;
import org.zinutils.bytecode.mock.IndentWriter;

public class JSModuleStmt implements IVForm {
	private final JSExpr obj;
	private final String javaIF;
	private final String jsName;

	public JSModuleStmt(JSExpr obj, String jsName, String javaIF) {
		this.obj = obj;
		this.javaIF = javaIF;
		this.jsName = jsName;
	}

	@Override
	public void write(IndentWriter w) {
		if (obj != null) {
			w.print(obj.asVar());
			w.print(".");
		}
		w.print("module('");
		w.print(jsName);
		w.print("')");
	}

	@Override
	public void generate(JVMCreationContext jvm) {
		NewMethodDefiner md = jvm.method();
		IExpr mod = md.castTo(md.callInterface(J.OBJECT, jvm.argAsIs(obj), "module", md.stringConst(jsName)), javaIF);
		jvm.local(this, mod);
	}

	@Override
	public void asivm(IVFWriter iw) {
		iw.println("module " + jsName);
	}
	
	@Override
	public String asVar() {
		// TODO Auto-generated method stub
		return null;
	}
}
