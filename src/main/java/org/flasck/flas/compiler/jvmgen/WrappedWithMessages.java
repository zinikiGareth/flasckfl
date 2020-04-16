package org.flasck.flas.compiler.jvmgen;

import org.flasck.jvm.J;
import org.zinutils.bytecode.IExpr;
import org.zinutils.bytecode.MethodDefiner;
import org.zinutils.bytecode.Var;
import org.zinutils.exceptions.NotImplementedException;

public class WrappedWithMessages implements IExpr {
	private final IExpr obj;

	public WrappedWithMessages(MethodDefiner meth, IExpr cx, Var v) {
		this.obj = meth.callStatic(J.RESPONSE_WITH_MESSAGES, J.OBJECT, "object", cx, meth.as(v, J.OBJECT));
	}

	@Override
	public void spitOutByteCode(MethodDefiner meth) {
		obj.spitOutByteCode(meth);
	}

	@Override
	public void flush() {
		throw new NotImplementedException();
	}

	@Override
	public String getType() {
		throw new NotImplementedException();
	}

	@Override
	public int asSource(StringBuilder sb, int ind, boolean b) {
		return obj.asSource(sb, ind, b);
	}
}
