package org.flasck.flas.compiler.jvmgen;

import java.util.ArrayList;
import java.util.List;

import org.flasck.flas.commonBase.Expr;
import org.flasck.flas.parsedForm.ut.UnitTestEvent;
import org.flasck.flas.repository.LeafAdapter;
import org.flasck.flas.repository.ResultAware;
import org.flasck.flas.repository.StackVisitor;
import org.zinutils.bytecode.IExpr;
import org.zinutils.bytecode.MethodDefiner;

public class DoUTEventGenerator extends LeafAdapter implements ResultAware {
	private final StackVisitor sv;
	private final FunctionState fs;
	private final IExpr runner;
	private final MethodDefiner meth;
	private List<IExpr> block = new ArrayList<>();
	private final List<IExpr> args = new ArrayList<>();

	public DoUTEventGenerator(StackVisitor sv, FunctionState fs, IExpr runner) {
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
		IExpr expr = (IExpr) r;
		this.args.add(expr);
	}
	
	@Override
	public void leaveUnitTestEvent(UnitTestEvent e) {
		if (args.size() != 2)
			throw new RuntimeException("expected card & event");
		this.meth.callInterface("void", runner, "event", args.get(0), args.get(1)).flush();
		sv.result(null);
	}

}
