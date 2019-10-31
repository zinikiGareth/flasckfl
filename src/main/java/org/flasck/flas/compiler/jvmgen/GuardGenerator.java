package org.flasck.flas.compiler.jvmgen;

import java.util.ArrayList;
import java.util.List;

import org.flasck.flas.commonBase.Expr;
import org.flasck.flas.parsedForm.FunctionCaseDefn;
import org.flasck.flas.parsedForm.FunctionIntro;
import org.flasck.flas.repository.LeafAdapter;
import org.flasck.flas.repository.NestedVisitor;
import org.flasck.flas.repository.ResultAware;
import org.flasck.jvm.J;
import org.zinutils.bytecode.IExpr;
import org.zinutils.bytecode.JavaType;
import org.zinutils.exceptions.NotImplementedException;

public class GuardGenerator extends LeafAdapter implements ResultAware {
	public class GE {
		public IExpr guard;
		public IExpr expr;

		public GE(IExpr guard, IExpr expr) {
			this.guard = guard;
			this.expr = expr;
		}
	}

	private final NestedVisitor sv;
	private final FunctionState state;
	private boolean isGuard;
	private IExpr currentGuard;
	private List<IExpr> block;
	private List<GE> stack = new ArrayList<>();

	public GuardGenerator(FunctionState state, NestedVisitor sv, List<IExpr> currentBlock) {
		this.state = state;
		this.sv = sv;
		this.block = currentBlock;
	}
	
	@Override
	public void visitGuard(FunctionCaseDefn c) {
		System.out.println("Visit Guard " + c.guard);
		isGuard = true;
		sv.push(new ExprGenerator(state, sv, this.block));
	}

	@Override
	public void visitExpr(Expr expr, int nArgs) {
		System.out.println("Main Expr");
		sv.push(new ExprGenerator(state, sv, new ArrayList<>()));
	}
	
	@Override
	public void endInline(FunctionIntro fi) {
		IExpr ret = null;
		GE c = stack.get(0);
		if (ret == null) {
			if (c.guard == null)
				ret = c.expr;
			else
				ret = state.meth.returnObject(state.meth.callStatic(J.ERROR, J.OBJECT, "eval", state.fcx, state.meth.arrayOf(J.OBJECT, state.meth.stringConst("no default guard"))));
		}
		ret = state.meth.ifBoolean(state.meth.callStatic(J.FLEVAL, JavaType.boolean_, "isTruthy", state.fcx, c.guard), c.expr, ret);
		sv.result(ret);
	}

	@Override
	public void result(Object r) {
		if (isGuard) {
			System.out.println("guard is " + r);
			isGuard = false;
			currentGuard = (IExpr) r;
//			JSIfExpr ifexpr = block.ifTrue((JSExpr) r);
//			trueblock = ifexpr.trueCase();
//			block = ifexpr.falseCase();
		} else {
			System.out.println("expr is " + r);
//			trueblock.returnObject((JSExpr) r);
			stack.add(0, new GE(currentGuard, (IExpr)r));
		}
	}
}
