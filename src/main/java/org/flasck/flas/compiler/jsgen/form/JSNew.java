package org.flasck.flas.compiler.jsgen.form;

import java.util.ArrayList;
import java.util.List;

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
		StringBuilder sb = new StringBuilder();
		sb.append("new ");
		sb.append(clz.jsName());
		sb.append("(_cxt");
		for (JSExpr e : args) {
			sb.append(", ");
			sb.append(e.asVar());
		}
		sb.append(")");
		return sb.toString();
	}

	@Override
	public void write(IndentWriter w) {
		w.print(asVar());
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
		} else
			clzName = clz.javaName();
		List<IExpr> stack = new ArrayList<IExpr>();
		stack.add(jvm.cxt());
		for (JSExpr a : args) {
			stack.add(jvm.arg(a));
		}
		IExpr ret = jvm.method().makeNew(clzName, stack.toArray(new IExpr[stack.size()]));
		jvm.local(this, ret);
	}

}
