package org.flasck.flas.compiler.jsgen.form;

import org.flasck.flas.compiler.jsgen.creators.JVMCreationContext;
import org.zinutils.bytecode.IExpr;
import org.zinutils.bytecode.mock.IndentWriter;

public class JSAssertion implements JSExpr {

	private final JSExpr obj;
	private final String meth;
	private final JSExpr[] args;

	public JSAssertion(JSExpr obj, String meth, JSExpr... args) {
		this.obj = obj;
		this.meth = meth;
		this.args = args;
	}

	@Override
	public void write(IndentWriter w) {
		if (obj != null) {
			obj.write(w);
			w.print(".");
		}
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
		IExpr ret = jvm.method().callInterface("void", jvm.argAsIs(obj), "assertSameValue", jvm.cxt(), jvm.arg(args[0]), jvm.arg(args[1]));
		jvm.local(this, ret);
	}
	
	@Override
	public String asVar() {
		// TODO Auto-generated method stub
		return null;
	}
}
