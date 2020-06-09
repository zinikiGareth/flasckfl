package org.flasck.flas.compiler.jvmgen;

import java.util.ArrayList;
import java.util.List;

import org.flasck.flas.commonBase.Expr;
import org.flasck.flas.parsedForm.TemplateStylingOption;
import org.flasck.flas.repository.LeafAdapter;
import org.flasck.flas.repository.ResultAware;
import org.flasck.flas.repository.StackVisitor;
import org.flasck.jvm.J;
import org.zinutils.bytecode.IExpr;

public class TemplateStyling extends LeafAdapter implements ResultAware {
	public enum Mode {
		COND, EXPR
	}
	private final FunctionState fs;
	private final StackVisitor sv;
	private final JVMBlockCreator currentBlock;
	private final List<IExpr> exprs = new ArrayList<>();
	private IExpr cond;
	private Mode mode;

	public TemplateStyling(FunctionState fs, StackVisitor sv, JVMBlockCreator bindingBlock, TemplateStylingOption tso) {
		this.fs = fs;
		this.sv = sv;
		this.currentBlock = bindingBlock;
		sv.push(this);
	}

	@Override
	public void visitTemplateStyleCond(Expr cond) {
		mode = Mode.COND;
		new ExprGenerator(fs, sv, currentBlock, false);
	}

	@Override
	public void visitTemplateStyleExpr(Expr expr) {
		mode = Mode.EXPR;
		new ExprGenerator(fs, sv, currentBlock, false);
	}
	
	@Override
	public void result(Object r) {
		if (mode == Mode.COND)
			cond = (IExpr) r;
		else
			exprs.add((IExpr)r);
	}

	@Override
	public void leaveTemplateStyling(TemplateStylingOption tso) {
		String c = tso.constant();
		IExpr ret;
		if (exprs.isEmpty())
			ret = fs.meth.stringConst(c);
		else {
			if (c != null)
				exprs.add(fs.meth.stringConst(c));
			ret = fs.meth.callStatic(J.BUILTINPKG+".PACKAGEFUNCTIONS", J.STRING, "concatMany", fs.fcx, fs.meth.arrayOf(J.OBJECT, exprs));
		}
		sv.result(new JVMStyleIf(cond, ret));
	}

}
