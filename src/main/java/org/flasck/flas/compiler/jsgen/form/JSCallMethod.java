package org.flasck.flas.compiler.jsgen.form;

import org.flasck.flas.compiler.jsgen.creators.JVMCreationContext;
import org.zinutils.bytecode.IExpr;
import org.zinutils.bytecode.mock.IndentWriter;

public class JSCallMethod implements JSExpr {
	private String returnType;
	private JSExpr obj;
	private String method;
	private final JSExpr[] args;

	public JSCallMethod(String returnType, JSExpr obj, String method, JSExpr... args) {
		this.returnType = returnType;
		this.obj = obj;
		this.method = method;
		this.args = args;
	}

	@Override
	public void write(IndentWriter w) {
		if (obj != null) {
			w.print(obj.asVar());
			w.print(".");
		}
		w.print(method);
		w.print("(_cxt");
		for (JSExpr e : args) {
			w.print(", ");
			w.print(e.asVar());
		}
		w.print(")");
		if (returnType.equals("void"))
			w.println(";");
	}

	@Override
	public String asVar() {
		throw new RuntimeException("This should be wrapped in a JSLocal or JSThis");
	}

	@Override
	public void generate(JVMCreationContext jvm) {
		IExpr ai = jvm.argAsIs(obj);
		IExpr[] ais = new IExpr[args.length+1];
		int ap = 0;
		ais[ap++] = jvm.cxt();
		for (JSExpr a : args) {
			ais[ap++] = jvm.arg(a);
		}
		IExpr mi = jvm.method().callInterface(returnType, ai, method, ais);
		jvm.local(this, mi);
	}
}
