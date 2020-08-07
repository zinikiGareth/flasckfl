package org.flasck.flas.compiler.jsgen.form;

import org.flasck.flas.commonBase.names.FunctionName;
import org.flasck.flas.compiler.jsgen.creators.JVMCreationContext;
import org.flasck.jvm.J;
import org.zinutils.bytecode.IExpr;
import org.zinutils.bytecode.NewMethodDefiner;
import org.zinutils.bytecode.mock.IndentWriter;

public class JSMakeAcor implements JSExpr {
	private final FunctionName acorMeth;
	private final JSExpr obj;
	private final int nargs;
	private String var;

	public JSMakeAcor(FunctionName acorMeth, JSExpr obj, int nargs) {
		this.acorMeth = acorMeth;
		this.obj = obj;
		this.nargs = nargs;
	}

	@Override
	public void write(IndentWriter w) {
		w.print("_cxt.mkacor(");
		w.print(acorMeth.jsPName());
		w.print(",");
		w.print(obj.asVar());
		w.print(",");
		w.print(Integer.toString(nargs));
		w.print(")");
	}

	@Override
	public String asVar() {
		return var;
	}

	@Override
	public void generate(JVMCreationContext jvm) {
		NewMethodDefiner md = jvm.method();
		IExpr mkacor = md.callInterface(J.OBJECT, jvm.cxt(), "mkacor", md.classConst(acorMeth.inContext.javaClassName()), md.stringConst(acorMeth.name), jvm.arg(obj), md.intConst(nargs));
		jvm.local(this, mkacor);
	}
}
