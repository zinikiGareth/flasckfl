package org.flasck.flas.compiler.jsgen.form;

import java.util.ArrayList;
import java.util.List;

import org.flasck.flas.compiler.jsgen.creators.JVMCreationContext;
import org.zinutils.bytecode.IExpr;
import org.zinutils.bytecode.JavaType;
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
			w.print(obj.asVar());
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
 		int ai = 0;
		for (JSExpr e : args) {
			if (("event".equals(meth) || "input".equals(meth) || "matchText".equals(meth) || "matchStyle".equals(meth) || "matchScroll".equals(meth) || "matchImageUri".equals(meth)) && ai == 1)
				as.add(jvm.argAs(e, new JavaType(List.class.getName())));
			else if (("matchText".equals(meth) || "matchStyle".equals(meth) || "matchScroll".equals(meth)) && ai == 2)
				as.add(jvm.argAs(e, JavaType.boolean_));
			else if (("matchText".equals(meth) || "matchStyle".equals(meth)) && ai == 3)
				as.add(jvm.argAs(e, JavaType.string));
			else if (("matchImageUri".equals(meth)) && ai == 2)
				as.add(jvm.argAs(e, JavaType.string));
			else if ("matchScroll".equals(meth) && ai == 3)
				as.add(jvm.argAs(e, new JavaType(Double.class.getName())));
			else if ("shove".equals(meth) && ai == 1)
				as.add(jvm.argAs(e, JavaType.string));
			else
				as.add(jvm.arg(e));
			ai++;
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
//		case "invoke": {
//			iw.println("invoke");
//			break;
//		}
//		case "matchText": {
//			iw.println("matchText");
//			break;
//		}
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
