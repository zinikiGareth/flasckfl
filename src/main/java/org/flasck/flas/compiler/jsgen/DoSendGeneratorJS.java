package org.flasck.flas.compiler.jsgen;

import java.util.ArrayList;
import java.util.List;

import org.flasck.flas.commonBase.Expr;
import org.flasck.flas.commonBase.names.NameOfThing;
import org.flasck.flas.compiler.jsgen.creators.JSBlockCreator;
import org.flasck.flas.compiler.jsgen.form.JSExpr;
import org.flasck.flas.parsedForm.ContractDecl;
import org.flasck.flas.parsedForm.UnresolvedVar;
import org.flasck.flas.parsedForm.ut.UnitTestSend;
import org.flasck.flas.repository.LeafAdapter;
import org.flasck.flas.repository.NestedVisitor;
import org.flasck.flas.repository.ResultAware;
import org.flasck.flas.tc3.NamedType;

public class DoSendGeneratorJS extends LeafAdapter implements ResultAware {
	private final JSFunctionState state;
	private final NestedVisitor sv;
	private final JSBlockCreator block;
	private final JSExpr runner;
	private JSExpr sendTo;
	private JSExpr contract;
	private JSExpr fn;
	private final List<JSExpr> args = new ArrayList<>();

	public DoSendGeneratorJS(JSFunctionState state, NestedVisitor sv, JSBlockCreator block, JSExpr runner) {
		this.state = state;
		this.sv = sv;
		this.block = block;
		this.runner = runner;
		sv.push(this);
	}
	
	@Override
	public void visitUnresolvedVar(UnresolvedVar var, int nargs) {
		new ExprGeneratorJS(state, sv, block, false).visitUnresolvedVar(var, nargs);
	}

	@Override
	public void visitSendMethod(NamedType defn, UnresolvedVar fn) {
		NameOfThing contract = ((ContractDecl) defn).name();
		this.contract = block.string(contract.uniqueName());
		this.fn = block.string(fn.var);
	}
	
	@Override
	public void visitExpr(Expr expr, int nArgs) {
		new ExprGeneratorJS(state, sv, block, false).visitExpr(expr, nArgs);
	}
	
	@Override
	public void leaveUnitTestSend(UnitTestSend s) {
		JSExpr sendArgs = block.makeArray(args.toArray(new JSExpr[args.size()]));
		block.assertable(runner, "send", this.sendTo, contract, fn, sendArgs);
		sv.result(null);
	}
	
	@Override
	public void result(Object r) {
		if (sendTo == null) {
			this.sendTo = (JSExpr) r;
		} else {
			this.args.add((JSExpr) r);
		}
	}

}
