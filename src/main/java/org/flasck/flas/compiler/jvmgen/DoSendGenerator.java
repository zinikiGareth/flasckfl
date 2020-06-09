package org.flasck.flas.compiler.jvmgen;

import java.util.ArrayList;
import java.util.List;

import org.flasck.flas.commonBase.Expr;
import org.flasck.flas.commonBase.names.NameOfThing;
import org.flasck.flas.parsedForm.ContractDecl;
import org.flasck.flas.parsedForm.UnresolvedVar;
import org.flasck.flas.parsedForm.ut.UnitTestSend;
import org.flasck.flas.repository.LeafAdapter;
import org.flasck.flas.repository.ResultAware;
import org.flasck.flas.repository.StackVisitor;
import org.flasck.flas.tc3.NamedType;
import org.flasck.jvm.J;
import org.zinutils.bytecode.IExpr;
import org.zinutils.bytecode.MethodDefiner;

public class DoSendGenerator extends LeafAdapter implements ResultAware {
	private final StackVisitor sv;
	private final FunctionState fs;
	private final IExpr runner;
	private final MethodDefiner meth;
	private final JVMBlockCreator block;
	private IExpr sendTo;
	private final List<IExpr> sendArgs = new ArrayList<>();
	private final List<IExpr> args = new ArrayList<>();

	public DoSendGenerator(StackVisitor sv, FunctionState fs, IExpr runner, JVMBlockCreator block) {
		this.sv = sv;
		this.fs = fs;
		this.runner = runner;
		this.block = block;
		this.meth = fs.meth;
		sv.push(this);
		this.sendArgs.add(fs.fcx);
	}
	
	@Override
	public void visitUnresolvedVar(UnresolvedVar var, int nargs) {
		new ExprGenerator(fs, sv, block, false).visitUnresolvedVar(var, nargs);
	}

	@Override
	public void visitSendMethod(NamedType defn, UnresolvedVar fn) {
		NameOfThing contract = ((ContractDecl) defn).name();
		this.sendArgs.add(meth.stringConst(contract.uniqueName()));
		this.sendArgs.add(meth.stringConst(fn.var));
	}
	
	@Override
	public void visitExpr(Expr expr, int nArgs) {
		new ExprGenerator(fs, sv, block, false).visitExpr(expr, nArgs);
	}
	
	@Override
	public void result(Object r) {
		IExpr expr = (IExpr) r;
		if (sendTo == null) {
			this.sendTo = expr;
			this.sendArgs.add(sendTo);
		} else {
			this.args.add(expr);
		}
	}
	
	@Override
	public void leaveUnitTestSend(UnitTestSend s) {
		sendArgs.add(this.meth.arrayOf(J.OBJECT, args.toArray(new IExpr[args.size()])));
		block.add(this.meth.callInterface("void", runner, "send", sendArgs.toArray(new IExpr[sendArgs.size()])));
		sv.result(null);
	}

}
