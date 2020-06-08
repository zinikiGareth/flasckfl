package org.flasck.flas.compiler.jsgen;

import java.util.Arrays;

import org.flasck.flas.commonBase.Expr;
import org.flasck.flas.commonBase.names.PackageName;
import org.flasck.flas.compiler.jsgen.creators.JSBlockCreator;
import org.flasck.flas.compiler.jsgen.form.JSExpr;
import org.flasck.flas.parsedForm.FunctionIntro;
import org.flasck.flas.repository.LeafAdapter;
import org.flasck.flas.repository.NestedVisitor;
import org.flasck.flas.repository.ResultAware;

public class ObjectCtorGeneratorJS extends LeafAdapter implements ResultAware {
	private final JSFunctionState state;
	private final NestedVisitor sv;
	private final JSBlockCreator meth;

	public ObjectCtorGeneratorJS(JSFunctionState state, NestedVisitor sv, JSBlockCreator block) {
		this.state = state;
		this.sv = sv;
		this.meth = block;
		if (block == null)
			throw new NullPointerException("Cannot have a null block");
		sv.push(this);
	}

	@Override
	public void visitExpr(Expr expr, int nArgs) {
		new ExprGeneratorJS(state, sv, meth, false);
	}
	
	@Override
	public void result(Object r) {
		this.meth.keepMessages(state.ocmsgs(), (JSExpr)r);
	}
	
	@Override
	public void endInline(FunctionIntro fi) {
		JSExpr returned = meth.newOf(new PackageName("ResponseWithMessages"), Arrays.asList(state.ocret(), state.ocmsgs()));
		meth.returnObject(returned);
		sv.result(null);
	}
}
