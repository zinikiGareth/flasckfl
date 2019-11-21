package org.flasck.flas.compiler.jvmgen;

import java.util.ArrayList;
import java.util.List;

import org.flasck.flas.repository.LeafAdapter;
import org.flasck.flas.repository.ResultAware;
import org.flasck.flas.repository.StackVisitor;
import org.flasck.jvm.J;
import org.zinutils.bytecode.IExpr;
import org.zinutils.bytecode.MethodDefiner;

public class DoInvocationGenerator extends LeafAdapter implements ResultAware {
	private final StackVisitor sv;
	private final IExpr cx;
	private final IExpr runner;
	private final MethodDefiner meth;
	private List<IExpr> block = new ArrayList<>();

	public DoInvocationGenerator(StackVisitor sv, FunctionState fs, IExpr runner) {
		this.sv = sv;
		this.runner = runner;
		this.meth = fs.meth;
		this.cx = fs.fcx;
		sv.push(this);
		new ExprGenerator(fs, sv, block);
	}

	@Override
	public void result(Object r) {
		IExpr expr = meth.as((IExpr) r, J.OBJECT);
		IExpr ret = meth.callVirtual("void", runner, "invoke", cx, expr);
		block.add(ret);
		sv.result(JVMGenerator.makeBlock(meth, block));
	}
}
