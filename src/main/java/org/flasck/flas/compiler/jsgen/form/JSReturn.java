package org.flasck.flas.compiler.jsgen.form;

import org.flasck.flas.compiler.jsgen.creators.JVMCreationContext;
import org.zinutils.bytecode.IExpr;
import org.zinutils.bytecode.NewMethodDefiner;
import org.zinutils.bytecode.mock.IndentWriter;

public class JSReturn implements IVForm {
	private final JSExpr jsExpr;

	public JSReturn(JSExpr jsExpr) {
		this.jsExpr = jsExpr;
	}

	@Override
	public String asVar() {
		return null;
	}

	@Override
	public void write(IndentWriter w) {
		w.print("return ");
		w.print(jsExpr.asVar());
		w.println(";");
	}
	
	@Override
	public void generate(JVMCreationContext jvm) {
		NewMethodDefiner md = jvm.method();
		if (!jvm.hasLocal(jsExpr))
			jsExpr.generate(jvm);
		IExpr ret = jvm.argAsIs(jsExpr);
		if ("boolean".contentEquals(ret.getType()))
			ret = md.returnBool(ret);
		else
			ret = md.returnObject(ret);
		jvm.local(this, ret);
	}

	@Override
	public void asivm(IVFWriter iw) {
		iw.println("return " + jsExpr.asVar());
	}
}
