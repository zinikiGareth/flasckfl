package org.flasck.flas.compiler.jvmgen;

import java.util.List;

import org.flasck.flas.repository.LeafAdapter;
import org.flasck.flas.repository.ResultAware;
import org.flasck.flas.repository.StackVisitor;
import org.flasck.jvm.J;
import org.zinutils.bytecode.IExpr;
import org.zinutils.bytecode.MethodDefiner;
import org.zinutils.bytecode.Var;

public class StructFieldGenerator extends LeafAdapter implements ResultAware {
	private final StackVisitor sv;
	private final String fieldName;
	private final MethodDefiner meth;
	private final Var ret;

	public StructFieldGenerator(FunctionState state, StackVisitor sv, List<IExpr> currentBlock, String fieldName) {
		this.sv = sv;
		this.fieldName = fieldName;
		this.meth = state.meth;
		this.ret = state.evalRet;
		sv.push(this);
		new ExprGenerator(state, sv, currentBlock, false);
	}

	@Override
	public void result(Object r) {
		IExpr svar = meth.getField(ret, "state");
		sv.result(meth.callInterface("void", svar, "set", meth.stringConst(fieldName), meth.as((IExpr)r, J.OBJECT)));
	}
}
