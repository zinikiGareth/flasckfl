package org.flasck.flas.rewriter;

import java.util.Set;

import org.flasck.flas.commonBase.ApplyExpr;
import org.flasck.flas.commonBase.BooleanLiteral;
import org.flasck.flas.commonBase.IfExpr;
import org.flasck.flas.commonBase.NumericLiteral;
import org.flasck.flas.commonBase.StringLiteral;
import org.flasck.flas.commonBase.template.TemplateListVar;
import org.flasck.flas.flim.BuiltinOperation;
import org.flasck.flas.rewrittenForm.AssertTypeExpr;
import org.flasck.flas.rewrittenForm.CardFunction;
import org.flasck.flas.rewrittenForm.CardMember;
import org.flasck.flas.rewrittenForm.CardStateRef;
import org.flasck.flas.rewrittenForm.FunctionLiteral;
import org.flasck.flas.rewrittenForm.HandlerLambda;
import org.flasck.flas.rewrittenForm.LocalVar;
import org.flasck.flas.rewrittenForm.ObjectReference;
import org.flasck.flas.rewrittenForm.PackageVar;
import org.flasck.flas.rewrittenForm.RWCastExpr;
import org.flasck.flas.rewrittenForm.SendExpr;
import org.flasck.flas.rewrittenForm.TypeCheckMessages;
import org.flasck.flas.rewrittenForm.ScopedVar;
import org.zinutils.reflection.Reflection;

public class GatherScopedVars {
	private Set<ScopedVar> scopedVars;

	public GatherScopedVars(Set<ScopedVar> scopedVars) {
		this.scopedVars = scopedVars;
	}

	public void dispatch(Object expr) {
		if (expr != null)
			Reflection.call(this, "process", expr);
	}
	
	public void process(ApplyExpr expr) {
		dispatch(expr.fn);
		for (Object o : expr.args)
			dispatch(o);
	}
	
	public void process(IfExpr expr) {
		dispatch(expr.guard);
		dispatch(expr.ifExpr);
		dispatch(expr.elseExpr);
	}
	
	public void process(SendExpr expr) {
		dispatch(expr.sender);
		for (Object o : expr.args)
			dispatch(o);
	}
	
	public void process(TypeCheckMessages expr) {
		dispatch(expr.expr);
	}
	
	public void process(AssertTypeExpr expr) {
		dispatch(expr.expr);
	}
	
	public void process(RWCastExpr expr) {
		dispatch(expr.expr);
	}
	
	public void process(StringLiteral sl) {
	}

	public void process(NumericLiteral nl) {
	}
	
	public void process(BooleanLiteral bl) {
	}
	
	public void process(LocalVar lv) {
	}
	
	public void process(PackageVar pv) {
	}
	
	public void process(BuiltinOperation expr) {
	}
	
	public void process(CardStateRef csr) {
	}
	
	public void process(CardMember cm) {
	}
	
	public void process(ObjectReference or) {
	}
	
	public void process(CardFunction cm) {
	}
	
	public void process(FunctionLiteral fl) {
	}
	
	public void process(TemplateListVar tlv) {
	}
	
	public void process(HandlerLambda hl) {
	}
	
	public void process(ScopedVar sv) {
		scopedVars.add(sv);
	}

}
