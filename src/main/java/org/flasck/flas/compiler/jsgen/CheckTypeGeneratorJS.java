package org.flasck.flas.compiler.jsgen;

import org.flasck.flas.commonBase.Expr;
import org.flasck.flas.compiler.jsgen.creators.JSBlockCreator;
import org.flasck.flas.compiler.jsgen.form.JSExpr;
import org.flasck.flas.parsedForm.CheckTypeExpr;
import org.flasck.flas.parsedForm.TypeReference;
import org.flasck.flas.repository.LeafAdapter;
import org.flasck.flas.repository.NestedVisitor;
import org.flasck.flas.repository.ResultAware;
import org.flasck.flas.tc3.NamedType;

public class CheckTypeGeneratorJS extends LeafAdapter implements ResultAware {
	private final JSFunctionState state;
	private final NestedVisitor sv;
	private final JSBlockCreator block;
	private final boolean isExpectation;
	private JSExpr res;
	private NamedType type;

	public CheckTypeGeneratorJS(JSFunctionState state, NestedVisitor sv, JSBlockCreator block, boolean isExpectation) {
		this.state = state;
		this.sv = sv;
		this.block = block;
		this.isExpectation = isExpectation;
		sv.push(this);
	}

	@Override
	public void visitTypeReference(TypeReference var, boolean expectPolys, int exprNargs) {
		this.type = var.namedDefn();
	}
	
	@Override
	public void visitExpr(Expr expr, int nArgs) {
		new ExprGeneratorJS(state, sv, block, isExpectation);
	}
	
	@Override
	public void result(Object r) {
		res = (JSExpr) r;
	}
	
	@Override
	public void leaveCheckTypeExpr(CheckTypeExpr expr) {
		sv.result(block.checkType(type, res));
	}
}
