package org.flasck.flas.compiler.jvmgen;

import java.util.ArrayList;
import java.util.List;

import org.flasck.flas.commonBase.Expr;
import org.flasck.flas.parsedForm.ut.UnitTestRender;
import org.flasck.flas.repository.LeafAdapter;
import org.flasck.flas.repository.ResultAware;
import org.flasck.flas.repository.StackVisitor;
import org.zinutils.bytecode.IExpr;
import org.zinutils.bytecode.MethodDefiner;

public class DoUTRenderGenerator extends LeafAdapter implements ResultAware {
	private final StackVisitor sv;
	private final FunctionState fs;
	private final IExpr runner;
	private final MethodDefiner meth;
	private final JVMBlockCreator block;
	private final List<IExpr> args = new ArrayList<>();

	public DoUTRenderGenerator(StackVisitor sv, FunctionState fs, IExpr runner, JVMBlockCreator block) {
		this.sv = sv;
		this.fs = fs;
		this.runner = runner;
		this.block = block;
		this.meth = fs.meth;
		sv.push(this);
	}
	
	@Override
	public void visitExpr(Expr expr, int nArgs) {
		new ExprGenerator(fs, sv, block, false).visitExpr(expr, nArgs);
	}
	
	@Override
	public void result(Object r) {
		IExpr expr = (IExpr) r;
		this.args.add(expr);
	}
	
	@Override
	public void leaveUnitTestRender(UnitTestRender r) {
		if (args.size() != 1)
			throw new RuntimeException("expected object");
		this.block.add(this.meth.callInterface("void", runner, "render", fs.fcx, args.get(0), this.meth.intConst(r.template.template().position()), this.meth.stringConst("items/" + r.template.template().name().baseName())));
		sv.result(null);
	}
}
