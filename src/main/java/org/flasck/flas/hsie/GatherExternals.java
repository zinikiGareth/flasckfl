package org.flasck.flas.hsie;

import org.flasck.flas.commonBase.ApplyExpr;
import org.flasck.flas.commonBase.ConstPattern;
import org.flasck.flas.commonBase.NumericLiteral;
import org.flasck.flas.commonBase.StringLiteral;
import org.flasck.flas.commonBase.template.TemplateListVar;
import org.flasck.flas.rewrittenForm.AssertTypeExpr;
import org.flasck.flas.rewrittenForm.CardStateRef;
import org.flasck.flas.rewrittenForm.ExternalRef;
import org.flasck.flas.rewrittenForm.FunctionLiteral;
import org.flasck.flas.rewrittenForm.IterVar;
import org.flasck.flas.rewrittenForm.LocalVar;
import org.flasck.flas.rewrittenForm.RWConstructorMatch;
import org.flasck.flas.rewrittenForm.RWConstructorMatch.Field;
import org.flasck.flas.rewrittenForm.RWFunctionCaseDefn;
import org.flasck.flas.rewrittenForm.RWFunctionDefinition;
import org.flasck.flas.rewrittenForm.RWTypedPattern;
import org.flasck.flas.rewrittenForm.RWVarPattern;
import org.flasck.flas.rewrittenForm.TypeCheckMessages;
import org.flasck.flas.rewrittenForm.VarNestedFromOuterFunctionScope;
import org.flasck.flas.typechecker.Type;
import org.flasck.flas.vcode.hsieForm.HSIEForm;
import org.zinutils.exceptions.UtilException;
import org.zinutils.reflection.Reflection;

@SuppressWarnings("unused") // many of the methods here appear to be unused, but they're really used by reflection
public class GatherExternals {
	private final HSIEForm ret;

	public GatherExternals(HSIEForm ret) {
		this.ret = ret;
	}

	public void process(RWFunctionDefinition defn) {
		for (RWFunctionCaseDefn cs : defn.cases)
			process(cs);
	}

	private void process(RWFunctionCaseDefn cs) {
		for (Object a : cs.args())
			dispatch(a);
		dispatch(cs.expr);
	}

	protected void dispatch(Object a) {
		try {
			Reflection.call(this, "process", a);
		} catch (UtilException ex) {
			System.out.println("Process: " + a.getClass());
		}
	}

	private void process(RWVarPattern v) {
		// no externals here
	}
	
	private void process(ConstPattern v) {
		if (v.type == ConstPattern.INTEGER) {
			ret.dependsOn("Number");
		} else if (v.type == ConstPattern.BOOLEAN) {
			ret.dependsOn("Boolean");
		} else
			throw new UtilException("HSIE Cannot handle constant pattern for " + v.type);
	}
	
	private void process(RWTypedPattern tp) {
		ret.dependsOn(tp.type.name());
	}
	
	private void process(RWConstructorMatch cm) {
		ret.dependsOn(cm.ref);
		for (Field a : cm.args)
			dispatch(a.patt);
	}
	
	private void process(ApplyExpr expr) {
		dispatch(expr.fn);
		for (Object o : expr.args)
			dispatch(o);
	}
	
	private void process(AssertTypeExpr expr) {
//		dispatch(expr.type);
		dispatch(expr.expr);
	}
	
	private void process(TypeCheckMessages expr) {
		dispatch(expr.expr);
	}
	
	private void process(VarNestedFromOuterFunctionScope vn) {
		if (!vn.definedLocally)
			ret.dependsOn(vn);
	}
	
	private void process(ExternalRef er) {
		ret.dependsOn(er);
	}
	
	private void process(LocalVar lv) {
		// no externals here
	}
	
	private void process(TemplateListVar lv) {
		// no externals here
	}
	
	private void process(IterVar lv) {
		// no externals here
	}
	
	private void process(CardStateRef lv) {
		// no externals here
	}
	
	private void process(NumericLiteral nl) {
		// no externals here
	}
	
	private void process(StringLiteral sl) {
		// no externals here
	}
	
	private void process(FunctionLiteral sl) { // surely this should be an externalref?
		// no externals here
	}
}
