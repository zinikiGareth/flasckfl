package org.flasck.flas.compiler.jsgen;

import org.flasck.flas.commonBase.Expr;
import org.flasck.flas.compiler.jsgen.creators.JSBlockCreator;
import org.flasck.flas.compiler.jsgen.form.JSExpr;
import org.flasck.flas.compiler.jsgen.form.JSString;
import org.flasck.flas.parsedForm.st.AjaxSubscribe;
import org.flasck.flas.repository.LeafAdapter;
import org.flasck.flas.repository.NestedVisitor;
import org.flasck.flas.repository.ResultAware;
import org.flasck.jvm.J;

public class AjaxExpectSubscribe extends LeafAdapter implements ResultAware {
	private final JSFunctionState state;
	private final NestedVisitor sv;
	private final JSBlockCreator block;
	private final JSExpr v;

	public AjaxExpectSubscribe(JSFunctionState state, NestedVisitor sv, JSBlockCreator block, JSExpr member, AjaxSubscribe as) {
		this.state = state;
		this.sv = sv;
		this.block = block;
		v = block.callMethod(J.AJAXEXPECTOR, member, "expectSubscribe", new JSString(as.pathUrl.text));
		sv.push(this);
	}
	
	@Override
	public void visitExpr(Expr expr, int nArgs) {
		new ExprGeneratorJS(state, sv, block, false);
	}
	
	@Override
	public void leaveAjaxExpectSubscribe(AjaxSubscribe as) {
		sv.result(null);
	}

	@Override
	public void result(Object r) {
		block.callMethod("void", v, "response", (JSExpr)r);
	}

}
