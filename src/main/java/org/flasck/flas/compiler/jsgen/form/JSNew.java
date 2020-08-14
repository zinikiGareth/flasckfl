package org.flasck.flas.compiler.jsgen.form;

import java.util.ArrayList;
import java.util.List;

import org.flasck.flas.commonBase.names.CSName;
import org.flasck.flas.commonBase.names.HandlerName;
import org.flasck.flas.commonBase.names.NameOfThing;
import org.flasck.flas.commonBase.names.PackageName;
import org.flasck.flas.compiler.jsgen.creators.JVMCreationContext;
import org.flasck.jvm.J;
import org.zinutils.bytecode.IExpr;
import org.zinutils.bytecode.mock.IndentWriter;
import org.zinutils.exceptions.NotImplementedException;

public class JSNew implements JSExpr {
	private final NameOfThing clz;
	private final List<JSExpr> args;

	public JSNew(NameOfThing clz) {
		this(clz, new ArrayList<>());
	}

	public JSNew(NameOfThing clz, List<JSExpr> args) {
		this.clz = clz;
		this.args = args;
	}

	@Override
	public String asVar() {
		throw new RuntimeException("This should be wrapped in a JSLocal or JSThis");
	}

	@Override
	public void write(IndentWriter w) {
		w.print("new ");
		w.print(clz.jsName());
		w.print("(_cxt");
		for (JSExpr e : args) {
			w.print(", ");
			w.print(e.asVar());
		}
		w.print(")");
	}

	@Override
	public void generate(JVMCreationContext jvm) {
		String clzName;
		if (clz instanceof PackageName) {
			switch (clz.baseName()) {
			case "ResponseWithMessages": {
				clzName = J.RESPONSE_WITH_MESSAGES;
				break;
			}
			default:
				throw new NotImplementedException("cannot handle builtin name " + clz);
			}
		} else if (clz instanceof CSName)
			clzName = clz.javaClassName();
		else
			clzName = clz.javaName();
		List<IExpr> stack = new ArrayList<IExpr>();
		stack.add(jvm.cxt());
		for (JSExpr a : args) {
			if (!jvm.hasLocal(a))
				a.generate(jvm);
			stack.add(jvm.arg(a));
		}
		IExpr ret = jvm.method().makeNew(clzName, stack.toArray(new IExpr[stack.size()]));
		jvm.local(this, ret);
	}

}
