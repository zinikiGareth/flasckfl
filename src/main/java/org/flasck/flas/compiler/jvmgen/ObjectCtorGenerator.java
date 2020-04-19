package org.flasck.flas.compiler.jvmgen;

import java.util.List;

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
	private final List<IExpr> currentBlock;
	private IExpr messages;

	public ObjectCtorGenerator(FunctionState fs, StackVisitor sv, List<IExpr> currentBlock) {
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
		messages = (IExpr) r;
	}
	
	@Override
	public void endInline(FunctionIntro fi) {
		MethodDefiner meth = state.meth;
		IExpr returned = meth.makeNew(J.RESPONSE_WITH_MESSAGES, state.fcx, meth.as(state.ocret(), J.OBJECT), meth.as(messages, J.OBJECT));
		currentBlock.add(meth.returnObject(returned));
		sv.result(null);
	}
}
