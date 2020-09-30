package org.flasck.flas.compiler.jsgen;

import org.flasck.flas.compiler.jsgen.creators.JSBlockCreator;
import org.flasck.flas.compiler.jsgen.form.JSTypeOf;
import org.flasck.flas.parsedForm.TypeExpr;
import org.flasck.flas.parsedForm.TypeReference;
import org.flasck.flas.parsedForm.UnresolvedVar;
import org.flasck.flas.repository.LeafAdapter;
import org.flasck.flas.repository.NestedVisitor;
import org.zinutils.exceptions.NotImplementedException;

public class TypeExprGeneratorJS extends LeafAdapter {
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
	public void visitTypeReference(TypeReference var, boolean expectPolys, int exprNargs) {
		expr = new JSTypeOf(var.defn());
	}
	
	@Override
	public void visitUnresolvedVar(UnresolvedVar var, int nargs) {
		throw new NotImplementedException("it seems reasonable to ask for the type of a var or expr");
	}
	
	
	@Override
	public void leaveTypeExpr(TypeExpr expr) {
		if (this.expr == null)
			throw new NotImplementedException("we didn't consider some (type ...) case");
		sv.result(this.expr);
	}
}
