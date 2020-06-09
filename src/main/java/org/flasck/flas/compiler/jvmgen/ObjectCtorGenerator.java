package org.flasck.flas.compiler.jvmgen;

import org.flasck.flas.commonBase.Expr;
import org.flasck.flas.parsedForm.FunctionIntro;
import org.flasck.flas.repository.LeafAdapter;
import org.flasck.flas.repository.ResultAware;
import org.flasck.flas.repository.StackVisitor;
import org.flasck.jvm.J;
import org.zinutils.bytecode.IExpr;
import org.zinutils.bytecode.MethodDefiner;

public class ObjectCtorGenerator extends LeafAdapter implements ResultAware {
	private final FunctionState state;
	private final StackVisitor sv;
	private final JVMBlockCreator currentBlock;

	public ObjectCtorGenerator(FunctionState fs, StackVisitor sv, JVMBlockCreator currentBlock) {
		this.state = fs;
		this.sv = sv;
		this.currentBlock = currentBlock;
		sv.push(this);
	}

	@Override
	public void visitExpr(Expr expr, int nArgs) {
		new ExprGenerator(state, sv, currentBlock, false);
	}
	
	@Override
	public void result(Object r) {
		IExpr messages = (IExpr) r;
		currentBlock.add(state.meth.callInterface("void", state.fcx, "addAll", state.ocmsgs(), state.meth.as(messages, J.OBJECT)));
	}
	
	@Override
	public void endInline(FunctionIntro fi) {
		MethodDefiner meth = state.meth;
		IExpr returned = meth.makeNew(J.RESPONSE_WITH_MESSAGES, state.fcx, meth.as(state.ocret(), J.OBJECT), meth.as(state.ocmsgs(), J.OBJECT));
		currentBlock.add(meth.returnObject(returned));
		sv.result(null);
	}
}
