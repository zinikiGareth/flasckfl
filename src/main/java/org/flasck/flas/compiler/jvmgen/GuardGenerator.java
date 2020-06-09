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

public class GuardGenerator extends LeafAdapter implements ResultAware {
	public class GE {
		public IExpr guard;
		public JVMBlockCreator testBlk;
		public IExpr expr;

		public GE(IExpr guard, JVMBlockCreator testBlk, IExpr expr) {
			this.guard = guard;
			this.testBlk = testBlk;
			this.expr = expr;
		}
	}

	private final NestedVisitor sv;
	private final FunctionState state;
	private final List<GE> stack = new ArrayList<>();
	private boolean isGuard;
	private IExpr currentGuard;
	private final JVMBlockCreator block;
	private JVMBlockCreator nestedBlk = null;
	private JVMBlockCreator testBlk = null;

	public GuardGenerator(FunctionState state, NestedVisitor sv, JVMBlockCreator currentBlock) {
		this.state = state;
		this.sv = sv;
		this.block = currentBlock;
		sv.push(this);
	}
	
	@Override
	public void visitGuard(FunctionCaseDefn c) {
		isGuard = true;
		new ExprGenerator(state, sv, testBlk != null ? testBlk : this.block, false);
	}

	@Override
	public void visitExpr(Expr expr, int nArgs) {
		JVMBlockCreator blk;
		if (currentGuard == null && stack.isEmpty())
			blk = this.block;
		else
			nestedBlk = blk = new JVMBlock(this.block);
		new ExprGenerator(state, sv, blk, false);
	}
	
	@Override
	public void endInline(FunctionIntro fi) {
		IExpr ret = null;
		for (GE c : stack) {
			if (ret == null) {
				if (c.guard == null) {
					ret = c.expr;
					continue;
				} else {
					ret = state.meth.returnObject(state.meth.callStatic(J.FLERROR, J.OBJECT, "eval", state.fcx, state.meth.arrayOf(J.OBJECT, state.meth.stringConst("no default guard"))));
				}
			}
			ret = state.meth.ifBoolean(state.meth.callInterface(JavaType.boolean_.toString(), state.fcx, "isTruthy", state.meth.as(c.guard, J.OBJECT)), c.expr, ret);
			if (c.testBlk != null) {
				c.testBlk.add(ret);
				ret = c.testBlk.convert();
			}
		}
		sv.result(ret);
	}

	@Override
	public void result(Object r) {
		IExpr re = (IExpr)r;
		if (isGuard) {
			isGuard = false;
			currentGuard = re;
		} else {
			re = state.meth.returnObject(re);
			if (nestedBlk != null) {
				nestedBlk.add(re);
				re = nestedBlk.convert();
			}
			stack.add(0, new GE(currentGuard, testBlk, re));
			currentGuard = null;
			testBlk = new JVMBlock(this.block);
		}
	}
}
