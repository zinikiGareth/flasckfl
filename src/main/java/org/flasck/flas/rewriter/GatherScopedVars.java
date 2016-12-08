package org.flasck.flas.rewriter;

import java.util.Set;

import org.flasck.flas.commonBase.ApplyExpr;
import org.flasck.flas.commonBase.NumericLiteral;
import org.flasck.flas.commonBase.StringLiteral;
import org.flasck.flas.rewrittenForm.HandlerLambda;
import org.flasck.flas.rewrittenForm.LocalVar;
import org.flasck.flas.rewrittenForm.PackageVar;
import org.flasck.flas.rewrittenForm.SendExpr;
import org.flasck.flas.rewrittenForm.TypeCheckMessages;
import org.flasck.flas.rewrittenForm.VarNestedFromOuterFunctionScope;
import org.zinutils.reflection.Reflection;

public class GatherScopedVars {
	private Set<VarNestedFromOuterFunctionScope> scopedVars;

	public GatherScopedVars(Set<VarNestedFromOuterFunctionScope> scopedVars) {
		this.scopedVars = scopedVars;
		// TODO Auto-generated constructor stub
	}

	public void dispatch(Object expr) {
		Reflection.call(this, "process", expr);
	}
	
	public void process(ApplyExpr expr) {
		dispatch(expr.fn);
		for (Object o : expr.args)
			dispatch(o);
	}
	
	public void process(SendExpr expr) {
		dispatch(expr.sender);
		for (Object o : expr.args)
			dispatch(o);
	}
	
	public void process(TypeCheckMessages expr) {
		dispatch(expr.expr);
	}
	
	public void process(StringLiteral sl) {
	}

	public void process(NumericLiteral nl) {
	}
	
	public void process(LocalVar lv) {
	}
	
	public void process(PackageVar pv) {
	}
	
	public void process(HandlerLambda hl) {
	}
	
	public void process(VarNestedFromOuterFunctionScope sv) {
		scopedVars.add(sv);
	}

}
