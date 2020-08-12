package org.flasck.flas.compiler.jsgen.form;

import java.util.ArrayList;
import java.util.List;

import org.flasck.flas.compiler.jsgen.creators.JVMCreationContext;
import org.zinutils.bytecode.IExpr;
import org.zinutils.bytecode.mock.IndentWriter;

public class JSAssertion implements IVForm {
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
 		List<IExpr> as = new ArrayList<>();
 		as.add(jvm.cxt());
		for (JSExpr e : args) {
			if (!jvm.hasLocal(e))
				e.generate(jvm);
			as.add(jvm.arg(e));
		}
		IExpr ret = jvm.method().callInterface("void", jvm.argAsIs(obj), meth, as.toArray(new IExpr[as.size()]));
		jvm.local(this, ret);
	}

	@Override
	public void asivm(IVFWriter iw) {
		switch (meth) {
		case "assertSameValue": {
			iw.println("assert same value");
			IVFWriter inner = iw.indent();
			for (JSExpr e : args) {
				if (e instanceof JSLocal)
					inner.println(e.asVar());
				else
					inner.write(e);
			}
			break;
		}
		default: {
			iw.println("assertion " + meth);
			IVFWriter inner = iw.indent();
			for (JSExpr e : args) {
				inner.write(e);
			}
		}
		}
	}
	
	@Override
	public String asVar() {
		// TODO Auto-generated method stub
		return null;
	}
}
