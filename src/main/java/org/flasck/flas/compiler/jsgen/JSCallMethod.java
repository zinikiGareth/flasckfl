package org.flasck.flas.compiler.jsgen;

import org.zinutils.bytecode.mock.IndentWriter;

public class JSCallMethod implements JSExpr {

	private final JSExpr obj;
	private final String meth;
	private final JSExpr[] args;

	public JSCallMethod(JSExpr obj, String meth, JSExpr... args) {
		this.obj = obj;
		this.meth = meth;
		this.args = args;
	}

	@Override
	public void write(IndentWriter w) {
		w.print("const v1 = ");
		obj.write(w);
		w.print(".");
		w.print(meth);
		w.print("(");
		boolean isFirst = true;
		for (JSExpr e : args) {
			if (isFirst)
				isFirst = false;
			else
				w.print(", ");
			w.print(e.asVar());
		}
		w.println(");");
	}

	@Override
	public String asVar() {
		// TODO Auto-generated method stub
		return null;
	}
}
