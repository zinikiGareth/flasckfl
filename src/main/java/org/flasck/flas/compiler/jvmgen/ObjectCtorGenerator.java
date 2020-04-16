package org.flasck.flas.compiler.jvmgen;

import java.util.List;

import org.flasck.flas.commonBase.Expr;
import org.flasck.flas.parsedForm.FunctionIntro;
import org.flasck.flas.parsedForm.ObjectContract;
import org.flasck.flas.parsedForm.ObjectDefn;
import org.flasck.flas.repository.LeafAdapter;
import org.flasck.flas.repository.ResultAware;
import org.flasck.flas.repository.StackVisitor;
import org.flasck.jvm.J;
import org.zinutils.bytecode.IExpr;
import org.zinutils.bytecode.MethodDefiner;
import org.zinutils.bytecode.Var;

public class ObjectCtorGenerator extends LeafAdapter implements ResultAware {
	private final FunctionState state;
	private final StackVisitor sv;
	private final ObjectDefn od;
	private final List<IExpr> currentBlock;
	private IExpr messages;

	public ObjectCtorGenerator(FunctionState fs, StackVisitor sv, ObjectDefn object, List<IExpr> currentBlock) {
		this.state = fs;
		this.sv = sv;
		this.od = object;
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
		Var ret = meth.avar(od.name().javaName(), "ret");
		IExpr created = meth.makeNew(od.name().javaName(), state.fcx);
		currentBlock.add(meth.assign(ret, created));
		int i = 0;
		for (ObjectContract oc : od.contracts) {
			IExpr ci = meth.arrayElt(state.fargs, meth.intConst(i++));
			IExpr assn = meth.assign(meth.getField(ret, oc.varName().var), ci);
			currentBlock.add(assn);
		}
		IExpr returned = meth.makeNew(J.RESPONSE_WITH_MESSAGES, state.fcx, meth.as(ret, J.OBJECT), meth.as(messages, J.OBJECT));
		currentBlock.add(meth.returnObject(returned));
		sv.result(null);
	}
}
