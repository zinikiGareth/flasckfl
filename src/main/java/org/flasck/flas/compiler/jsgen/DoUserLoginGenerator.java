package org.flasck.flas.compiler.jsgen;

import java.util.ArrayList;
import java.util.List;

import org.flasck.flas.commonBase.Expr;
import org.flasck.flas.compiler.jsgen.creators.JSBlockCreator;
import org.flasck.flas.compiler.jsgen.form.JSExpr;
import org.flasck.flas.parsedForm.st.UserLogin;
import org.flasck.flas.repository.LeafAdapter;
import org.flasck.flas.repository.NestedVisitor;
import org.flasck.flas.repository.ResultAware;

public class DoUserLoginGenerator extends LeafAdapter implements ResultAware {
	private final JSFunctionState state;
	private final NestedVisitor sv;
	private final JSBlockCreator block;
	private final JSExpr runner;
	private final List<JSExpr> args = new ArrayList<>();

	public DoUserLoginGenerator(JSFunctionState state, NestedVisitor sv, JSBlockCreator block, JSExpr runner) {
		this.state = state;
		this.sv = sv;
		this.block = block;
		this.runner = runner;
		sv.push(this);
	}

	@Override
	public void visitExpr(Expr expr, int nArgs) {
		new ExprGeneratorJS(state, sv, block, true);
	}
	
	@Override
	public void result(Object r) {
		args.add((JSExpr)r);
	}
	
	@Override
	public void leaveUserLogin(UserLogin ul) {
		block.assertable(runner, "userlogin", args.get(0), args.get(1));
		sv.result(null);
	}
}
