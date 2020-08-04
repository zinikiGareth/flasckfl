package org.flasck.flas.compiler.jsgen.form;

import org.flasck.flas.compiler.jsgen.creators.JSMethod;
import org.flasck.flas.compiler.jsgen.creators.JVMCreationContext;
import org.zinutils.bytecode.mock.IndentWriter;

public class JSLocal implements JSExpr {
	private final JSMethod meth;
	private final JSExpr value;
	private String var;

	public JSLocal(JSMethod meth, JSExpr value) {
		this.meth = meth;
		this.value = value;
	}

	@Override
	public String asVar() {
		if (var == null)
			var = meth.obtainNextVar();
		return var;
	}

	@Override
	public void write(IndentWriter w, JVMCreationContext jvm) {
		if (var == null)
			var = meth.obtainNextVar();
		w.print("const ");
		w.print(var);
		w.print(" = ");
		if (jvm != null)
			jvm.assignTo(this);
		System.out.println("local has class " + value.getClass());
		value.write(w, jvm);
		w.println(";");
	}
}
