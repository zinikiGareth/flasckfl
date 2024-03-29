package org.flasck.flas.compiler.jsgen;

import org.flasck.flas.commonBase.Expr;
import org.flasck.flas.compiler.jsgen.creators.JSBlockCreator;
import org.flasck.flas.compiler.jsgen.creators.JSIfCreator;
import org.flasck.flas.compiler.jsgen.form.JSExpr;
import org.flasck.flas.parsedForm.FunctionCaseDefn;
import org.flasck.flas.parsedForm.FunctionIntro;
import org.flasck.flas.repository.LeafAdapter;
import org.flasck.flas.repository.NestedVisitor;
import org.flasck.flas.repository.ResultAware;

public class GuardGeneratorJS extends LeafAdapter implements ResultAware {
	private final JSFunctionState state;
	private final NestedVisitor sv;
	private JSBlockCreator block;
	private boolean isGuard, seenGuard;
	private JSBlockCreator trueblock;

	public GuardGeneratorJS(JSFunctionState state, NestedVisitor sv, JSBlockCreator block) {
		this.state = state;
		this.sv = sv;
		if (block == null)
			throw new NullPointerException("Cannot have a null block");
		this.block = block;
		this.trueblock = block;
	}
	
	@Override
	public void visitGuard(FunctionCaseDefn c) {
		isGuard = true;
		seenGuard = true;
		new ExprGeneratorJS(state, sv, this.block, false);
	}

	@Override
	public void visitExpr(Expr expr, int nArgs) {
		if (!seenGuard)
			trueblock = block;
		new ExprGeneratorJS(state, sv, trueblock, false);
		seenGuard = false;
	}
	
	@Override
	public void endInline(FunctionIntro fi) {
		if (block != trueblock)
			block.errorNoDefaultGuard();
		sv.result(null);
	}

	@Override
	public void result(Object r) {
		if (isGuard) {
			isGuard = false;
			JSIfCreator ifexpr = block.ifTrue((JSExpr) r);
			trueblock = ifexpr.trueCase();
			block = ifexpr.falseCase();
		} else {
			if (state.shouldCacheResult()) {
				state.meth().cacheResult((JSExpr) r);
			}
			trueblock.returnObject((JSExpr) r);
		}
	}
}
