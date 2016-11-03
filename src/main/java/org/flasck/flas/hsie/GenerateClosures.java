package org.flasck.flas.hsie;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.flasck.flas.commonBase.ApplyExpr;
import org.flasck.flas.commonBase.CastExpr;
import org.flasck.flas.commonBase.LocatedObject;
import org.flasck.flas.commonBase.NumericLiteral;
import org.flasck.flas.commonBase.StringLiteral;
import org.flasck.flas.commonBase.template.TemplateListVar;
import org.flasck.flas.errors.ErrorResult;
import org.flasck.flas.rewrittenForm.AssertTypeExpr;
import org.flasck.flas.rewrittenForm.CardFunction;
import org.flasck.flas.rewrittenForm.CardMember;
import org.flasck.flas.rewrittenForm.CardStateRef;
import org.flasck.flas.rewrittenForm.FunctionLiteral;
import org.flasck.flas.rewrittenForm.HandlerLambda;
import org.flasck.flas.rewrittenForm.IterVar;
import org.flasck.flas.rewrittenForm.LocalVar;
import org.flasck.flas.rewrittenForm.ObjectReference;
import org.flasck.flas.rewrittenForm.PackageVar;
import org.flasck.flas.rewrittenForm.TypeCheckMessages;
import org.flasck.flas.rewrittenForm.VarNestedFromOuterFunctionScope;
import org.flasck.flas.typechecker.Type;
import org.flasck.flas.vcode.hsieForm.ClosureCmd;
import org.flasck.flas.vcode.hsieForm.CreationOfVar;
import org.flasck.flas.vcode.hsieForm.HSIEBlock;
import org.flasck.flas.vcode.hsieForm.HSIEForm;
import org.zinutils.exceptions.UtilException;
import org.zinutils.reflection.Reflection;

public class GenerateClosures {
	private final ErrorResult errors;
	private final MetaState ms;
	private final HSIEForm form;
	private /* final */ Map<String, CreationOfVar> substs;

	public GenerateClosures(ErrorResult errors, MetaState ms, HSIEForm form) {
		this.errors = errors;
		this.ms = ms;
		this.form = form;
	}

	// TODO: I would like the substs to just "be a field" common across all expressions and passed into the constructor
	public void generateClosure(Map<String, CreationOfVar> substs, Object expr) {
		this.substs = substs;
		ms.translate(expr, dispatch(expr));
	}

	private LocatedObject dispatch(Object expr) {
		if (expr == null)
			return new LocatedObject(null, null);
		try {
			return Reflection.call(this, "process", expr);
		} catch (UtilException ex) {
			System.out.println("Process: " + expr.getClass());
			throw ex;
		}
	}

	public LocatedObject process(ApplyExpr expr) {
		List<LocatedObject> ops = new ArrayList<LocatedObject>();
		LocatedObject val = dispatch(expr.fn);
		if (val.obj instanceof CreationOfVar && expr.args.isEmpty()) {
			return val;
		}
		ops.add(val);
		for (Object o : expr.args) {
			ops.add(dispatch(o));
		}
		// TODO: check this doesn't already exist
		ClosureCmd closure = form.createClosure(expr.location);
		for (int i=0;i<ops.size();i++) {
			LocatedObject o = ops.get(i);
			closure.push(o.loc, o.obj);
			if (o.obj instanceof CreationOfVar) {
				CreationOfVar cov = (CreationOfVar) o.obj;
				ClosureCmd c2 = form.getClosure(cov.var);
				if (c2 != null) {
					closure.depends.addAll(c2.depends);
					closure.depends.add((CreationOfVar) o.obj);
				}
			}
		}
		return new LocatedObject(expr.location, new CreationOfVar(closure.var, expr.location, "clos" + closure.var.idx));
	}

	public LocatedObject process(PackageVar pv) {
		return new LocatedObject(pv.location, pv);
	}

	public LocatedObject process(LocalVar nl) {
		String var = nl.uniqueName();
		if (!substs.containsKey(var))
			throw new UtilException("How can this be a local var? " + var + " not in " + substs);
		return new LocatedObject(nl.varLoc, substs.get(var));
	}

	public LocatedObject process(VarNestedFromOuterFunctionScope sv) {
		String var = sv.id;
		if (!sv.definedLocally) {
			return new LocatedObject(sv.location, sv);
		}
		if (substs.containsKey(var))
			return new LocatedObject(sv.location, substs.get(var));
		throw new UtilException("Scoped var " + var + " not in " + substs + " for " + form.fnName);
	}

	public LocatedObject process(IterVar expr) {
		String var = expr.var;
		if (!substs.containsKey(var))
			throw new UtilException("How can this be an iter var? " + var + " not in " + substs);
		return new LocatedObject(expr.location, substs.get(var));
	}

	public LocatedObject process(CardMember expr) {
		return new LocatedObject(expr.location(), expr);
	}
	
	public LocatedObject process(CardStateRef expr) {
		return new LocatedObject(expr.location(), expr);
	}

	public LocatedObject process(CardFunction expr) {
		return new LocatedObject(expr.location(), expr);
	}

	public LocatedObject process(FunctionLiteral expr) {
		return new LocatedObject(expr.location(), expr);
	}

	public LocatedObject process(HandlerLambda expr) {
		return new LocatedObject(expr.location(), expr);
	}

	public LocatedObject process(TemplateListVar expr) {
		return new LocatedObject(expr.location, expr);
	}

	public LocatedObject process(NumericLiteral expr) {
		return new LocatedObject(expr.location, Integer.parseInt(expr.text));
	}

	public LocatedObject process(StringLiteral expr) {
		return new LocatedObject(expr.location, expr);
	}

	public LocatedObject process(ObjectReference expr) {
		return new LocatedObject(expr.location(), expr);
	}

	// 20016-11-03: Note that this is not currently in any of our test cases
	public LocatedObject process(CastExpr ce) {
		LocatedObject lo = dispatch(ce.expr);
		CreationOfVar cv = (CreationOfVar) lo.obj;
		HSIEBlock closure = form.getClosure(cv.var);
		closure.downcastType = (Type) ((PackageVar)ce.castTo).defn;
		return lo;
	}
	
	public LocatedObject process(TypeCheckMessages tcm) {
		LocatedObject lo = dispatch(tcm.expr);
		CreationOfVar cv = (CreationOfVar) lo.obj;
		ClosureCmd closure = form.getClosure(cv.var);
		closure.typecheckMessages = true;
		return lo;
	}
	
	public LocatedObject process(AssertTypeExpr ate) {
		LocatedObject conv = dispatch(ate.expr);
		if (conv.obj instanceof CreationOfVar) { // it's a closure, delegate to typechecker ..
			CreationOfVar cv = (CreationOfVar) conv.obj;
			ClosureCmd closure = form.getClosure(cv.var);
			closure.assertType = ate.type;
			return conv;
		} else if (conv.obj instanceof StringLiteral) {
			if (!ate.type.name().equals("String")) {
				errors.message(ate.location(), "cannot assign a string to " + ate.type.name());
			}
			return conv;
		} else
			throw new UtilException("We should check " + conv + " against " + ate.type);
	}
}
