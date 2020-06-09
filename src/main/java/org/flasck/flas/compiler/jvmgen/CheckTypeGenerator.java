package org.flasck.flas.compiler.jvmgen;

import org.flasck.flas.commonBase.Expr;
import org.flasck.flas.parsedForm.CheckTypeExpr;
import org.flasck.flas.parsedForm.TypeReference;
import org.flasck.flas.repository.LeafAdapter;
import org.flasck.flas.repository.NestedVisitor;
import org.flasck.flas.repository.ResultAware;
import org.flasck.flas.tc3.NamedType;
import org.flasck.jvm.J;
import org.zinutils.bytecode.IExpr;

public class CheckTypeGenerator extends LeafAdapter implements ResultAware {
	private final FunctionState state;
	private final NestedVisitor sv;
	private final JVMBlockCreator currentBlock;
	private final boolean isExpectation;
	private IExpr res;
	private NamedType type;

	public CheckTypeGenerator(FunctionState state, NestedVisitor sv, JVMBlockCreator currentBlock, boolean isExpectation) {
		this.state = state;
		this.sv = sv;
		this.currentBlock = currentBlock;
		this.isExpectation = isExpectation;
		sv.push(this);
	}

	@Override
	public void visitTypeReference(TypeReference var, boolean expectPolys) {
		this.type = var.defn();
	}
	
	@Override
	public void visitExpr(Expr expr, int nArgs) {
		new ExprGenerator(state, sv, currentBlock, isExpectation);
	}
	
	@Override
	public void result(Object r) {
		res = (IExpr) r;
	}
	
	@Override
	public void leaveCheckTypeExpr(CheckTypeExpr expr) {
		sv.result(state.meth.box(state.meth.callInterface(J.BOOLEANP.getActual(), state.fcx, "isA", res, state.meth.stringConst(type.name().uniqueName()))));
	}
}
