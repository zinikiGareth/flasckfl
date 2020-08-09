package org.flasck.flas.compiler.jsgen.form;

import java.util.ArrayList;
import java.util.List;

import org.flasck.flas.compiler.jsgen.creators.JVMCreationContext;
import org.flasck.jvm.J;
import org.zinutils.bytecode.IExpr;
import org.zinutils.bytecode.NewMethodDefiner;
import org.zinutils.bytecode.mock.IndentWriter;
import org.zinutils.exceptions.NotImplementedException;

public class JSExpectation implements JSExpr {
	private final JSExpr mock;
	private final String method;
	private final List<JSExpr> args;
	private final JSExpr handler;

	public JSExpectation(JSExpr mock, String method, List<JSExpr> args, JSExpr handler) {
		this.mock = mock;
		this.method = method;
		this.args = args;
		this.handler = handler;
	}

	@Override
	public String asVar() {
		throw new NotImplementedException();
	}

	@Override
	public void write(IndentWriter w) {
		w.print(mock.asVar());
		w.print(".expect(");
		w.print("'" + method + "'");
		w.print(", [");
		String sep = "";
		for (JSExpr a : args) {
			w.print(sep);
			sep = ", ";
			w.print(a.asVar());
		}
		w.print("], ");
		w.print(handler.asVar());
		w.println(");");
	}

	@Override
	public void generate(JVMCreationContext jvm) {
		NewMethodDefiner md = jvm.method();
		if (!jvm.hasLocal(mock))
			mock.generate(jvm);
		List<IExpr> stack = new ArrayList<>();
		for (JSExpr je : args) {
			if (!jvm.hasLocal(je))
				je.generate(jvm);
			stack.add(jvm.arg(je));
		}
		if (!jvm.hasLocal(handler)) {
			if (handler != null)
				handler.generate(jvm);
			else
				jvm.local(handler, md.aNull());
		}
		IExpr mk = md.castTo(jvm.argAsIs(mock), J.EXPECTING);
		IExpr x = md.voidExpr(md.callInterface(J.MOCKEXPECTATION, mk, "expect", md.stringConst(method), md.arrayOf(J.OBJECT, stack), jvm.arg(handler)));
		jvm.local(this, x);
	}

}
