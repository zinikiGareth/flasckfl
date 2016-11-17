package org.flasck.flas.hsie;

import java.util.Collection;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import org.flasck.flas.commonBase.ApplyExpr;
import org.flasck.flas.commonBase.ConstPattern;
import org.flasck.flas.commonBase.IfExpr;
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
import org.flasck.flas.types.Type;
import org.flasck.flas.rewrittenForm.RWFunctionCaseDefn;
import org.flasck.flas.rewrittenForm.RWFunctionDefinition;
import org.flasck.flas.rewrittenForm.RWTypedPattern;
import org.flasck.flas.rewrittenForm.RWVarPattern;
import org.flasck.flas.rewrittenForm.TypeCheckMessages;
import org.flasck.flas.rewrittenForm.VarNestedFromOuterFunctionScope;
import org.flasck.flas.vcode.hsieForm.HSIEForm;
import org.zinutils.exceptions.UtilException;
import org.zinutils.reflection.Reflection;

@SuppressWarnings("unused") // many of the methods here appear to be unused, but they're really used by reflection
public class GatherExternals {
	private HSIEForm curr;

	public GatherExternals() {
	}

	public void process(HSIEForm form, RWFunctionDefinition defn) {
		curr = form;
		for (RWFunctionCaseDefn cs : defn.cases)
			process(cs);
		curr = null;
	}

	public static void transitiveClosure(Map<String, HSIEForm> forms, Collection<HSIEForm> curr) {
		boolean again = true;
		while (again) {
			again = false;
			for (HSIEForm x : curr) {
				for (String s : x.externals)
					again |= include(forms, x, s);
				for (VarNestedFromOuterFunctionScope vn : x.scoped)
					again |= include(forms, x, vn.id);
				for (VarNestedFromOuterFunctionScope vn : x.scopedDefinitions)
					again |= include(forms, x, vn.id);
			}
		}
	}

	private static boolean include(Map<String, HSIEForm> forms, HSIEForm x, String s) {
		boolean again = false;
		HSIEForm d = forms.get(s);
		if (d == null) {
			return again;
		}
		for (VarNestedFromOuterFunctionScope nv : d.scoped)
			again |= x.dependsOn(nv);
		return again;
	}

	public static Set<VarNestedFromOuterFunctionScope> allScopedFrom(Map<String, HSIEForm> forms, HSIEForm form) {
		TreeSet<VarNestedFromOuterFunctionScope> ret = new TreeSet<>(new ExternalRef.Comparator());
		ret.addAll(form.scoped);
		for (VarNestedFromOuterFunctionScope vn : form.scopedDefinitions) {
			HSIEForm of = forms.get(vn.id);
			if (of != null)
				ret.addAll(of.scoped);
		}
		return ret;
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
			throw ex;
		}
	}

	private void process(RWVarPattern v) {
		// no externals here
	}
	
	private void process(ConstPattern v) {
		if (v.type == ConstPattern.INTEGER) {
			curr.dependsOn("Number");
		} else if (v.type == ConstPattern.BOOLEAN) {
			curr.dependsOn("Boolean");
		} else
			throw new UtilException("HSIE Cannot handle constant pattern for " + v.type);
	}
	
	private void process(RWTypedPattern tp) {
		curr.dependsOn(tp.type.name());
	}
	
	private void process(RWConstructorMatch cm) {
		curr.dependsOn(cm.ref);
		for (Field a : cm.args)
			dispatch(a.patt);
	}
	
	private void process(ApplyExpr expr) {
		dispatch(expr.fn);
		for (Object o : expr.args)
			dispatch(o);
	}
	
	private void process(IfExpr expr) {
		dispatch(expr.guard);
		dispatch(expr.ifExpr);
		dispatch(expr.elseExpr);
	}
	
	private void process(AssertTypeExpr expr) {
//		dispatch(expr.type);
		dispatch(expr.expr);
	}
	
	private void process(TypeCheckMessages expr) {
		dispatch(expr.expr);
	}
	
	private void process(ExternalRef er) {
		if (er instanceof VarNestedFromOuterFunctionScope) {
			VarNestedFromOuterFunctionScope vn = (VarNestedFromOuterFunctionScope) er;
			if (!vn.definedLocally)
				curr.dependsOn(vn);
			else
				curr.definesScoped(vn);
		}
		curr.dependsOn(er);
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
