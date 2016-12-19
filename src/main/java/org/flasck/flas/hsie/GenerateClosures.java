package org.flasck.flas.hsie;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.commonBase.ApplyExpr;
import org.flasck.flas.commonBase.BooleanLiteral;
import org.flasck.flas.commonBase.IfExpr;
import org.flasck.flas.commonBase.Locatable;
import org.flasck.flas.commonBase.LocatedObject;
import org.flasck.flas.commonBase.NumericLiteral;
import org.flasck.flas.commonBase.StringLiteral;
import org.flasck.flas.commonBase.template.TemplateListVar;
import org.flasck.flas.errors.ErrorResult;
import org.flasck.flas.rewriter.Rewriter;
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
import org.flasck.flas.rewrittenForm.RWCastExpr;
import org.flasck.flas.rewrittenForm.RWFunctionDefinition;
import org.flasck.flas.rewrittenForm.RWHandlerImplements;
import org.flasck.flas.rewrittenForm.SendExpr;
import org.flasck.flas.rewrittenForm.TypeCheckMessages;
import org.flasck.flas.rewrittenForm.ScopedVar;
import org.flasck.flas.types.Type;
import org.flasck.flas.vcode.hsieForm.ClosureCmd;
import org.flasck.flas.vcode.hsieForm.HSIEForm;
import org.flasck.flas.vcode.hsieForm.VarInSource;
import org.zinutils.exceptions.UtilException;
import org.zinutils.reflection.Reflection;

public class GenerateClosures {
	private final ErrorResult errors;
	private final Rewriter rw;
	private final CurrentFunction cf;
	private final Expressions ms;
	private final Map<String, VarInSource> substs;
	private final Map<String, HSIEForm> forms;
	private final HSIEForm form;

	public GenerateClosures(ErrorResult errors, Rewriter rw, CurrentFunction cf, Map<String, HSIEForm> forms, HSIEForm form) {
		this.errors = errors;
		this.rw = rw;
		this.cf = cf;
		this.ms = cf.expressions;
		this.substs = cf.substs;
		this.forms = forms;
		this.form = form;
	}

	public void generateScopingClosures() {
		Set<ScopedVar> allScoped = new TreeSet<>(form.scoped);
		allScoped.addAll(form.scopedDefinitions);
		Map<String, ClosureCmd> map = new HashMap<>();
		for (ScopedVar sv : allScoped) {
			if (!sv.definedBy.equals(form.funcName))
				continue;
			if (sv.defn instanceof LocalVar)
				continue;
			if (sv.defn instanceof RWHandlerImplements) {
				boolean needClos = false;
				RWHandlerImplements hi = (RWHandlerImplements) sv.defn;
				for (HandlerLambda bv : hi.boundVars) {
					needClos |= bv.scopedFrom != null;
				}
				if (!needClos)
					continue;
			}
			ClosureCmd clos = form.createClosure(sv.location);
			clos.justScoping = true;
			String id = sv.myId.uniqueName();
			clos.push(sv.location, new PackageVar(sv.location, id, null));
			cf.mapVar(id, new VarInSource(clos.var, sv.location, id));
			map.put(id, clos);
		}
		for (ScopedVar sv : allScoped) {
			if (sv.defn instanceof LocalVar)
				continue;
			String id = sv.myId.uniqueName();
			ClosureCmd clos = map.get(id);
			if (clos == null)
				continue;
			HSIEForm fn = forms.get(id);
			if (fn != null /* && fn.scoped.isEmpty() */)  {// The case where there are no scoped vars is degenerate, but easier to deal with like this
				for (ScopedVar i : fn.scoped) {
					pushThing(ms, form, map, clos, i);
				}
			} else if (sv.defn instanceof RWHandlerImplements) {
				RWHandlerImplements hi = (RWHandlerImplements) sv.defn;
				for (HandlerLambda bv : hi.boundVars) {
					if (bv.scopedFrom == null)
						continue;
					pushThing(ms, form, map, clos, bv.scopedFrom);
				}
			} else 
				throw new UtilException("What is this?" + sv.defn.getClass());
		}
	}

	public void generateExprClosures() {
		for (Object expr : ms.exprs())
			generateClosure(expr);
	}

	private void pushThing(Expressions ms, HSIEForm form, Map<String, ClosureCmd> map, ClosureCmd clos, ScopedVar i) {
		String id = i.myId.uniqueName();
		if (map.containsKey(id)) {
			VarInSource cov = new VarInSource(map.get(id).var, i.location, id);
			clos.push(i.location, cov);
			clos.depends.add(cov);
			return;
		}
		if (form.isDefinedByMe(i)) {
			if (i.defn instanceof LocalVar) {
				clos.push(i.location, cf.getSubst(((LocalVar)i.defn).uniqueName()));
			} else if (i.defn instanceof RWHandlerImplements) {
				// if it needs args, it will have been added to "map"
				clos.push(i.location, new PackageVar(i.location, id, null));
			}
			else if (i.defn instanceof RWFunctionDefinition)
				clos.push(i.location, new PackageVar(i.location, id, null));
			else
				throw new UtilException("Cannot handle " + i.defn + " of class " + i.defn.getClass());
		} else
			clos.push(i.location, i);
	}

	private void generateClosure(Object expr) {
		ms.translate(expr, dispatch(expr));
	}

	private LocatedObject dispatch(Object expr) {
		if (expr == null)
			return new LocatedObject(null, null);
		try {
			return Reflection.call(this, "process", expr);
		} catch (UtilException ex) {
			System.out.println("Error during closure generation of: " + expr.getClass());
			throw ex;
		}
	}

	public LocatedObject process(IfExpr expr) {
		ms.translate(expr.guard, dispatch(expr.guard));
		ms.translate(expr.ifExpr, dispatch(expr.ifExpr));
		ms.translate(expr.elseExpr, dispatch(expr.elseExpr));
		return new LocatedObject(null, new UtilException("You cannot use this value"));
	}
	
	public LocatedObject process(ApplyExpr expr) {
		List<LocatedObject> ops = new ArrayList<LocatedObject>();
		LocatedObject val = dispatch(expr.fn);
		if (val.obj instanceof VarInSource && expr.args.isEmpty()) {
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
			if (o.obj instanceof VarInSource) {
				VarInSource cov = (VarInSource) o.obj;
				ClosureCmd c2 = form.getClosure(cov.var);
				if (c2 != null) {
					closure.depends.addAll(c2.depends);
					closure.depends.add((VarInSource) o.obj);
				}
			}
		}
		return new LocatedObject(expr.location, new VarInSource(closure.var, expr.location, "clos" + closure.var.idx));
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

	public LocatedObject process(ScopedVar sv) {
		if (!sv.definedBy.equals(form.funcName)) {
			return new LocatedObject(sv.location, sv);
		}
		String var = sv.myId.uniqueName();
		if (substs.containsKey(var))
			return new LocatedObject(sv.location, substs.get(var));
		throw new UtilException("Scoped var " + var + " not in " + substs + " for " + form.funcName);
	}

	public LocatedObject process(IterVar expr) {
		String var = expr.uniqueName();
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

	public LocatedObject process(BooleanLiteral expr) {
		return new LocatedObject(expr.location(), expr);
	}

	public LocatedObject process(ObjectReference expr) {
		return new LocatedObject(expr.location(), expr);
	}

	public LocatedObject process(RWCastExpr ce) {
		LocatedObject lo = dispatch(ce.expr);
		VarInSource cv = (VarInSource) lo.obj;
		ClosureCmd closure = form.getClosure(cv.var);
		if (closure == null) {
			closure = form.createClosure(lo.loc);
			closure.push(lo.loc, cv);
			lo = new LocatedObject(lo.loc, new VarInSource(closure.var, lo.loc, "clos" + closure.var.idx));
		}
		closure.downcastType = (Type) ((PackageVar)ce.castTo).defn;
		return lo;
	}
	
	public LocatedObject process(TypeCheckMessages tcm) {
		LocatedObject lo = dispatch(tcm.expr);
		VarInSource cv = (VarInSource) lo.obj;
		ClosureCmd closure = form.getClosure(cv.var);
		closure.typecheckMessages = true;
		return lo;
	}
	
	public LocatedObject process(AssertTypeExpr ate) {
		LocatedObject conv = dispatch(ate.expr);
		if (conv.obj instanceof VarInSource) { // it's a closure, delegate to typechecker ..
			VarInSource cv = (VarInSource) conv.obj;
			form.varConstraints.add(cv, ate.type);
			return conv;
		} else if (conv.obj instanceof StringLiteral) {
			if (!ate.type.name().equals("String")) {
				errors.message(ate.location(), "cannot assign a string to " + ate.type.name());
			}
			return conv;
		} else if (conv.obj instanceof NumericLiteral || conv.obj instanceof Integer) {
			if (!ate.type.name().equals("Number")) {
				errors.message(ate.location(), "cannot assign a number to " + ate.type.name());
			}
			return conv;
		} else if (conv.obj instanceof BooleanLiteral) {
			if (!ate.type.name().equals("Boolean")) {
				errors.message(ate.location(), "cannot assign a boolean to " + ate.type.name());
			}
			return conv;
		} else
			throw new UtilException("You haven't covered the case for " + conv.obj + (conv.obj != null ? " of " + conv.obj.getClass() : ""));
	}

	public LocatedObject process(SendExpr dse) {
		PackageVar send = rw.getMe(dse.location(), "Send");
		form.dependsOn(send);
		ApplyExpr expr = new ApplyExpr(dse.location(), send, dse.sender, dse.method, asList(dse.location(), dse.args));
		LocatedObject conv = dispatch(expr);
		VarInSource cv = (VarInSource) conv.obj;
		ClosureCmd closure = form.getClosure(cv.var);
		closure.checkSend = true;
		return conv;
	}
	

	private Object asList(InputPosition loc, List<Object> args) {
		Object ret = rw.getMe(loc, "Nil");
		for (int n = args.size()-1;n>=0;n--) {
			Locatable arg = (Locatable) args.get(n);
			ret = new ApplyExpr(arg.location(), rw.getMe(loc, "Cons"), arg, ret);
		}
		return ret;
	}
}
