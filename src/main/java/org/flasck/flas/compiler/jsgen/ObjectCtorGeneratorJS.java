package org.flasck.flas.compiler.jsgen;

import java.util.Arrays;

import org.flasck.flas.commonBase.Expr;
import org.flasck.flas.commonBase.names.PackageName;
import org.flasck.flas.compiler.jsgen.creators.JSMethodCreator;
import org.flasck.flas.compiler.jsgen.form.JSExpr;
import org.flasck.flas.parsedForm.FunctionIntro;
import org.flasck.flas.parsedForm.ObjectDefn;
import org.flasck.flas.repository.LeafAdapter;
import org.flasck.flas.repository.NestedVisitor;
import org.flasck.flas.repository.ResultAware;

public class ObjectCtorGeneratorJS extends LeafAdapter implements ResultAware {
	private final JSFunctionState state;
	private final NestedVisitor sv;
	private final JSMethodCreator meth;
	private final JSExpr created;
	private JSExpr messages;

	public ObjectCtorGeneratorJS(JSFunctionState state, NestedVisitor sv, ObjectDefn od, JSMethodCreator meth, JSExpr ocret) {
		this.state = state;
		this.sv = sv;
		this.meth = meth;
		this.created = ocret;
		if (meth == null)
			throw new NullPointerException("Cannot have a null block");
		sv.push(this);
	}

	@Override
	public void visitExpr(Expr expr, int nArgs) {
		new ExprGeneratorJS(state, sv, meth, false);
	}
	
	@Override
	public void result(Object r) {
		messages = (JSExpr) r;
	}
	
	@Override
	public void endInline(FunctionIntro fi) {
		JSExpr returned = meth.newOf(new PackageName("ResponseWithMessages"), Arrays.asList(created, messages));
		meth.returnObject(returned);
		sv.result(null);
	}
}
