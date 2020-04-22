package org.flasck.flas.compiler.jvmgen;

import java.util.ArrayList;
import java.util.List;

import org.flasck.flas.commonBase.Expr;
import org.flasck.flas.parsedForm.ut.UnitTestMatch;
import org.flasck.flas.repository.LeafAdapter;
import org.flasck.flas.repository.ResultAware;
import org.flasck.flas.repository.StackVisitor;
import org.flasck.jvm.J;
import org.zinutils.bytecode.IExpr;
import org.zinutils.bytecode.MethodDefiner;

public class DoUTMatchGenerator extends LeafAdapter implements ResultAware {
	private final StackVisitor sv;
	private final FunctionState fs;
	private final IExpr runner;
	private final MethodDefiner meth;
	private List<IExpr> block = new ArrayList<>();
	private final List<IExpr> args = new ArrayList<>();

	public DoUTMatchGenerator(StackVisitor sv, FunctionState fs, IExpr runner) {
		this.sv = sv;
		this.fs = fs;
		this.runner = runner;
		this.meth = fs.meth;
		sv.push(this);
	}
	
	@Override
	public void visitExpr(Expr expr, int nArgs) {
		new ExprGenerator(fs, sv, block, false).visitExpr(expr, nArgs);
	}
	
	@Override
	public void result(Object r) {
		this.args.add((IExpr) r);
	}
	
	@Override
	public void leaveUnitTestMatch(UnitTestMatch m) {
		if (args.size() != 1)
			throw new RuntimeException("expected card");
		this.meth.callInterface("void", runner, "match", fs.fcx, args.get(0), this.meth.as(m.selector != null ? this.meth.stringConst(m.selector.text) : this.meth.aNull(), J.STRING), this.meth.boolConst(m.contains), this.meth.stringConst(m.text)).flush();
		sv.result(null);
	}

}
