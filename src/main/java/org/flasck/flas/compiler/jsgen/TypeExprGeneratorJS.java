package org.flasck.flas.compiler.jsgen;

import org.flasck.flas.commonBase.ApplyExpr;
import org.flasck.flas.commonBase.Expr;
import org.flasck.flas.commonBase.MemberExpr;
import org.flasck.flas.compiler.jsgen.creators.JSBlockCreator;
import org.flasck.flas.compiler.jsgen.form.JSExpr;
import org.flasck.flas.compiler.jsgen.form.JSTypeOf;
import org.flasck.flas.parsedForm.TypeExpr;
import org.flasck.flas.parsedForm.TypeReference;
import org.flasck.flas.repository.LeafAdapter;
import org.flasck.flas.repository.NestedVisitor;
import org.flasck.flas.repository.ResultAware;
import org.zinutils.exceptions.CantHappenException;

public class TypeExprGeneratorJS extends LeafAdapter implements ResultAware {
	private final JSFunctionState state;
	private final NestedVisitor sv;
	private final JSBlockCreator block;
	private JSTypeOf expr;

	public TypeExprGeneratorJS(JSFunctionState state, NestedVisitor sv, JSBlockCreator block) {
		this.state = state;
		this.sv = sv;
		this.block = block;
		sv.push(this);
	}

	@Override
	public void visitExpr(Expr expr, int nArgs) {
		if (expr instanceof TypeReference)
			return; // handled here directly
		else if (expr instanceof MemberExpr && ((MemberExpr)expr).boundEarly() && ((MemberExpr)expr).defn() instanceof TypeReference)
			return; // handled here directly
		else {
			ExprGeneratorJS ej = new ExprGeneratorJS(state, sv, block, false);
			ej.visitExpr(expr, nArgs);
		}
	}
	
	@Override
	public boolean visitMemberExpr(MemberExpr expr, int nargs) {
		if (!expr.boundEarly())
			throw new CantHappenException("I think our test above should stop this happening");
		TypeReference tr = (TypeReference)expr.defn();
		this.expr = new JSTypeOf(tr.defn());
		return true;
	}
	
	@Override
	public void visitTypeReference(TypeReference var, boolean expectPolys, int exprNargs) {
		expr = new JSTypeOf(var.defn());
	}
	
	@Override
	public void visitTypeExpr(TypeExpr expr) {
		new TypeExprGeneratorJS(state, sv, block);
	}
	
	@Override
	public void visitApplyExpr(ApplyExpr expr) {
		new ApplyExprGeneratorJS(state, sv, block);
	}

	@Override
	public void result(Object r) {
		if (this.expr != null)
			throw new CantHappenException("set expr twice");
		this.expr = new JSTypeOf((JSExpr)r);
	}
	
	@Override
	public void leaveTypeExpr(TypeExpr expr) {
		if (this.expr == null)
			throw new CantHappenException("we didn't consider some (type ...) case");
		sv.result(this.expr);
	}
}
