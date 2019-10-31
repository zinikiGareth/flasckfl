package org.flasck.flas.compiler.jsgen;

import org.flasck.flas.commonBase.Expr;
import org.flasck.flas.parsedForm.FunctionCaseDefn;
import org.flasck.flas.repository.LeafAdapter;
import org.flasck.flas.repository.NestedVisitor;
import org.flasck.flas.repository.ResultAware;

public class GuardGeneratorJS extends LeafAdapter implements ResultAware {
	private final NestedVisitor sv;
	private final JSBlockCreator block;
	private boolean isGuard;

	public GuardGeneratorJS(NestedVisitor sv, JSBlockCreator block) {
		this.sv = sv;
		this.block = block;
	}
	
	@Override
	public void visitGuard(FunctionCaseDefn c) {
		System.out.println("Visit Guard");
		isGuard = true;
		sv.push(new ExprGeneratorJS(sv, this.block));
	}

	@Override
	public void visitExpr(Expr expr, int nArgs) {
		System.out.println("Main Expr");
		sv.push(new ExprGeneratorJS(sv, this.block));
	}

	@Override
	public void result(Object r) {
		if (isGuard) {
			System.out.println("guard is " + r);
			isGuard = false;
		} else {
			sv.result(r);
			System.out.println("expr is " + r);
		}
	}
}
