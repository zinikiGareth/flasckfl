package org.flasck.flas.hsie;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;

import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.commonBase.ApplyExpr;
import org.flasck.flas.commonBase.CastExpr;
import org.flasck.flas.commonBase.IfExpr;
import org.flasck.flas.commonBase.NumericLiteral;
import org.flasck.flas.commonBase.StringLiteral;
import org.flasck.flas.commonBase.template.TemplateListVar;
import org.flasck.flas.rewriter.Rewriter;
import org.flasck.flas.rewrittenForm.CardFunction;
import org.flasck.flas.rewrittenForm.CardMember;
import org.flasck.flas.rewrittenForm.CardStateRef;
import org.flasck.flas.rewrittenForm.ExternalRef;
import org.flasck.flas.rewrittenForm.FunctionLiteral;
import org.flasck.flas.rewrittenForm.HandlerLambda;
import org.flasck.flas.rewrittenForm.IterVar;
import org.flasck.flas.rewrittenForm.LocalVar;
import org.flasck.flas.rewrittenForm.MethodInContext;
import org.flasck.flas.rewrittenForm.ObjectReference;
import org.flasck.flas.rewrittenForm.PackageVar;
import org.flasck.flas.rewrittenForm.RWFunctionCaseDefn;
import org.flasck.flas.rewrittenForm.RWFunctionDefinition;
import org.flasck.flas.rewrittenForm.RWHandlerImplements;
import org.flasck.flas.rewrittenForm.RWMethodCaseDefn;
import org.flasck.flas.rewrittenForm.RWMethodDefinition;
import org.flasck.flas.rewrittenForm.RWMethodMessage;
import org.flasck.flas.rewrittenForm.RWStructDefn;
import org.flasck.flas.rewrittenForm.VarNestedFromOuterFunctionScope;
import org.flasck.flas.typechecker.Type;
import org.flasck.flas.vcode.hsieForm.ClosureCmd;
import org.flasck.flas.vcode.hsieForm.CreationOfVar;
import org.flasck.flas.vcode.hsieForm.HSIEBlock;
import org.flasck.flas.vcode.hsieForm.HSIEForm;
import org.flasck.flas.vcode.hsieForm.Var;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.zinutils.exceptions.UtilException;

public class MetaState {
	static final Logger logger = LoggerFactory.getLogger("HSIE");

	public class TrailItem {
		private ClosureCmd closure;
		private TreeSet<VarNestedFromOuterFunctionScope> avars;
		private List<CreationOfVar> depends;

		public TrailItem(List<CreationOfVar> depends, ClosureCmd closure, TreeSet<VarNestedFromOuterFunctionScope> avars) {
			this.depends = depends;
			this.closure = closure;
			this.avars = avars;
		}
		
		@Override
		public String toString() {
			return closure.var + " " + avars;
		}
	}

	public class LocatedObject {
		InputPosition loc;
		Object obj;

		public LocatedObject(InputPosition loc, Object obj) {
			this.loc = loc;
			this.obj = obj;
		}
	}

	private final Rewriter rewriter;
	public final HSIEForm form;
	final List<State> allStates = new ArrayList<State>();
	private final Map<Var, Map<String, Var>> fieldVars = new HashMap<Var, Map<String, Var>>();
	private final Map<Object, LocatedObject> retValues = new HashMap<Object, LocatedObject>();
	private final Map<Var, List<CreationOfVar>> closureDepends = new HashMap<Var, List<CreationOfVar>>();

	public MetaState(Rewriter rewriter, Map<String, HSIEForm> previous, HSIEForm form) {
		this.rewriter = rewriter;
		this.form = form;
	}

	public void add(State s) {
		allStates.add(s);
	}

	public boolean allDone() {
		return allStates.isEmpty();
	}

	public State first() {
		return allStates.remove(0);
	}

	public Var allocateVar() {
		return form.allocateVar();
	}

	public Var varFor(Var from, String field) {
		if (!fieldVars.containsKey(from))
			fieldVars.put(from, new HashMap<String, Var>());
		if (!fieldVars.get(from).containsKey(field))
			fieldVars.get(from).put(field, allocateVar());
		Var ret = fieldVars.get(from).get(field);
//		System.out.println("Allocating " + ret + " for " + from + "." + field);
		return ret;
	}

	public void writeExpr(SubstExpr se, HSIEBlock writeTo) {
		writeIfExpr(se.substs, se.expr, writeTo);
	}
	
	private void writeIfExpr(Map<String, CreationOfVar> substs, Object expr, HSIEBlock writeTo) {
		logger.info("Handling " + form.fnName + "; expr = " + expr + "; substs = " + substs);
		// First handle the explicit "if" and "let" cases
		if (expr instanceof IfExpr) {
			IfExpr ae = (IfExpr) expr;
			List<InputPosition> elocs = new ArrayList<InputPosition>();
			HSIEBlock ifCmd = writeTo.ifCmd(ae.location(), (CreationOfVar) convertValue(elocs, substs, ae.guard));
			writeIfExpr(substs, ae.ifExpr, ifCmd);
			if (ae.elseExpr != null)
				writeIfExpr(substs, ae.elseExpr, writeTo);
			else
				writeTo.caseError();
			return;
		}
		
		// Now handle scoping by resolving the vars that are included in scope 
		List<TrailItem> tis = new ArrayList<TrailItem>();
		TreeSet<VarNestedFromOuterFunctionScope> set = new TreeSet<VarNestedFromOuterFunctionScope>();
		gatherScopedVars(set, expr);
		
		logger.info(form.fnName + " claims to have " + set + " scoped vars");
		
		// Transitively close the set
		TreeSet<VarNestedFromOuterFunctionScope> newOnes = new TreeSet<VarNestedFromOuterFunctionScope>(set);
		while (!newOnes.isEmpty()) {
			TreeSet<VarNestedFromOuterFunctionScope> discovered = new TreeSet<VarNestedFromOuterFunctionScope>();
			for (VarNestedFromOuterFunctionScope sv : newOnes) {
				TreeSet<VarNestedFromOuterFunctionScope> avars = new TreeSet<VarNestedFromOuterFunctionScope>();
				if (sv.defn instanceof LocalVar)
					continue;
				else if (sv.defn instanceof RWFunctionDefinition) {
					gatherScopedVars(avars, rewriter.functions.get(sv.id));
				} else if (sv.defn instanceof RWMethodDefinition) {
					gatherScopedVars(avars, rewriter.standalone.get(sv.id).method);
				} else if (sv.defn instanceof MethodInContext) {
					gatherScopedVars(avars, ((MethodInContext)sv.defn).method);
				} else if (sv.defn instanceof RWHandlerImplements) {
					gatherScopedVars(avars, rewriter.callbackHandlers.get(sv.id));
				} else
					throw new UtilException("Not handling " + sv.id + " of class " + sv.defn.getClass());
				for (VarNestedFromOuterFunctionScope o : avars)
					if (!set.contains(o))
						discovered.add(o);
			}
			set.addAll(discovered);
			newOnes = discovered;
		}
		logger.info("Once closed, has " + set + " scoped vars");
		
		// For each scoped var that we need, make sure that it a variable is allocated in the scope of the root function
		// which represents the function partially applied to the scoped-in parameters, capturing all the inter-definitional
		// dependencies in "TrailItems" (not a good name)
		for (VarNestedFromOuterFunctionScope sv : set) {
			if (sv.defn instanceof LocalVar) {
				// This test can't be applied, because the scoped vars in method messages
				// (which are HSIEd and typechecked "before" method conversion (i.e. during it)
				// don't have the scoped vars in the list.
				// Adding them "in all cases" makes for way too many args to the method.
				// TODO: rationalize this at some point, possibly during typechecker rewrite
//				if (!substs.containsKey(sv.id))
//					throw new UtilException("Cannot find local var " + sv.id + " in " + substs.keySet());
						
				logger.info("Ignoring scoped local var " + sv.id + " which will be added later in method message cases");
				continue;
			}
			if (!definedLocally(sv)) {
				logger.info("!!" + sv.id + " not defined locally to " + form.fnName);
				continue;
			}
			Var cv = form.allocateVar();
			ClosureCmd closure = form.closure(sv.location, cv);
			TreeSet<VarNestedFromOuterFunctionScope> avars = new TreeSet<VarNestedFromOuterFunctionScope>();
			if (sv.defn instanceof RWMethodDefinition) {
				closure.push(sv.location, new PackageVar(sv.location, sv.id, sv.defn));
				gatherScopedVars(avars, rewriter.standalone.get(sv.id).method);
				closure.justScoping = true;
			} else if (sv.defn instanceof MethodInContext) {
				RWMethodDefinition m = ((MethodInContext)sv.defn).method;
				closure.push(sv.location, new PackageVar(sv.location, sv.id, m));
				gatherScopedVars(avars, m);
				closure.justScoping = true;
			} else if (sv.defn instanceof RWFunctionDefinition) {
				closure.push(sv.location, new PackageVar(sv.location, sv.id, sv.defn));
				gatherScopedVars(avars, rewriter.functions.get(sv.id));
				closure.justScoping = true;
			} else if (sv.defn instanceof RWHandlerImplements) {
				closure.push(sv.location, new PackageVar(sv.location, sv.id, sv.defn));
				gatherScopedVars(avars, rewriter.callbackHandlers.get(sv.id));
			} else if (sv.defn instanceof LocalVar) {
				closure.push(sv.location, substs.get(sv.id));
			} else
				throw new UtilException("Cannot handle " + sv.id + " of type " + sv.defn.getClass());
			CreationOfVar cov = new CreationOfVar(cv, sv.location, sv.id);
			logger.info("Allocating " + cov.var + " for " + sv.id);
			substs.put(sv.id, cov);
			closureDepends.put(cov.var, new ArrayList<CreationOfVar>());
			tis.add(new TrailItem(closureDepends.get(cov.var), closure, avars));
		}
		
		// Now go back through all the closures we just created, adding in their dependencies so that everything
		// ends up getting generated
		for (TrailItem ti : tis) {
			logger.debug("Adding dependencies to closure " + ti.closure.var + ": " + ti.avars);
			for (VarNestedFromOuterFunctionScope av : ti.avars) {
				if (!substs.containsKey(av.id)) {
					logger.info("Ignoring " + av.id + " because there is no rewritten value for it; it presumably is going to be passed in");
					continue;
				}
				CreationOfVar cov = substs.get(av.id);
				if (closureDepends.containsKey(cov.var)) {
					logger.debug("Adding closure " + cov.var + " to " + ti.closure.var);
					ti.depends.add(cov);
				}
				ti.closure.push(av.location, cov);
			}
		}
		writeFinalExpr(substs, expr, writeTo);
	}

	private boolean definedLocally(VarNestedFromOuterFunctionScope sv) {
		if (sv.id.length() < form.fnName.length()+1)
			return false;
		String s = sv.id.substring(form.fnName.length());
		if (s.charAt(0) == '_')
			s = s.substring(s.indexOf("."));
		return s.indexOf(".", 1) == -1;
	}

	public void writeFinalExpr(Map<String, CreationOfVar> substs, Object expr, HSIEBlock writeTo) {
		LocatedObject lo = getValueFor(substs, expr);
		writeTo.doReturn(lo.loc, lo.obj, closureDependencies(lo.obj));
	}

	public LocatedObject getValueFor(Map<String, CreationOfVar> substs, Object e) {
		if (!retValues.containsKey(e)) {
			List<InputPosition> elocs = new ArrayList<InputPosition>();
			Object val = convertValue(elocs, substs, e);
			retValues.put(e, new LocatedObject(elocs.get(0), val));
		}
		return retValues.get(e);
	}

	private Object convertValue(List<InputPosition> locs, Map<String, CreationOfVar> substs, Object expr) {
		if (expr == null) { // mainly error trapping, but valid in if .. if .. <no else> case
			locs.add(null);
			return null;
		} else if (expr instanceof NumericLiteral) {
			locs.add(((NumericLiteral)expr).location);
			return Integer.parseInt(((NumericLiteral)expr).text); // what about floats?
		} else if (expr instanceof StringLiteral) {
			locs.add(((StringLiteral)expr).location);
			return expr;
		} else if (expr instanceof FunctionLiteral) {
			locs.add(((FunctionLiteral)expr).location);
			return expr;
		} else if (expr instanceof TemplateListVar) {
			locs.add(((TemplateListVar)expr).location);
			return expr;
		} else if (expr instanceof LocalVar) {
			locs.add(((LocalVar)expr).varLoc);
			String var = ((LocalVar)expr).uniqueName();
			if (!substs.containsKey(var))
				throw new UtilException("How can this be a local var? " + var + " not in " + substs);
			return substs.get(var);
		} else if (expr instanceof IterVar) {
			locs.add(((IterVar)expr).location);
			String var = ((IterVar)expr).var;
			if (!substs.containsKey(var))
				throw new UtilException("How can this be an iter var? " + var + " not in " + substs);
			return substs.get(var);
		} else if (expr instanceof PackageVar) {
			// a package var is a reference to an absolute something that is referenced by its full scope
			PackageVar pv = (PackageVar)expr;
			locs.add(pv.location);
			form.dependsOn(pv);
			return expr;
		} else if (expr instanceof RWStructDefn) {
			RWStructDefn sd = (RWStructDefn) expr;
			locs.add(sd.location());
			form.dependsOn(sd.name());
			return expr;
		} else if (expr instanceof VarNestedFromOuterFunctionScope) {
			VarNestedFromOuterFunctionScope sv = (VarNestedFromOuterFunctionScope)expr;
			locs.add(sv.location);
			String var = sv.id;
			if (!sv.definedLocally) {
				form.dependsOn(sv);
				return sv;
			}
			if (substs.containsKey(var))
				return substs.get(var);
			throw new UtilException("Scoped var " + var + " not in " + substs + " for " + form.fnName);
		} else if (expr instanceof ObjectReference || expr instanceof CardFunction) {
			locs.add(((ExternalRef)expr).location());
			form.dependsOn(expr);
			return expr;
		} else if (expr instanceof CardMember) {
			locs.add(((ExternalRef)expr).location());
			form.dependsOn(expr);
			return expr;
		} else if (expr instanceof CardStateRef) {
			locs.add(((CardStateRef)expr).location());
			return expr;
		} else if (expr instanceof HandlerLambda) {
			locs.add(((ExternalRef)expr).location());
			form.dependsOn(expr);
			return expr;
		} else if (expr instanceof ApplyExpr) {
			ApplyExpr e2 = (ApplyExpr) expr;
			List<Object> ops = new ArrayList<Object>();
			List<InputPosition> elocs = new ArrayList<InputPosition>();
			Object val = convertValue(elocs, substs, e2.fn);
			if (val instanceof CreationOfVar && e2.args.isEmpty()) {
				locs.add(e2.location);
				return val;
			}
			ops.add(val);
			for (Object o : e2.args)
				ops.add(convertValue(elocs, substs, o));
			// TODO: check this doesn't already exist
			Var var = allocateVar();
			HSIEBlock closure = form.closure(e2.location, var);
			List<CreationOfVar> mydeps = new ArrayList<CreationOfVar>();
			if (ops.size() != elocs.size())
				throw new UtilException("Misplaced location or operation: " +  elocs.size() + " != " + ops.size());
			for (int i=0;i<ops.size();i++) {
				Object o = ops.get(i);
				if (elocs.get(i) == null) {
					System.out.println("Failed to find location for " + i + " in:");
					System.out.println("   -> " + e2);
					System.out.println("   -> " + ops);
					System.out.println("   -> " + elocs);
				}
				closure.push(elocs.get(i), o);
				if (o instanceof CreationOfVar && closureDepends.containsKey(o) && !mydeps.contains(o)) {
					mydeps.addAll(closureDepends.get(o));
					mydeps.add((CreationOfVar) o);
				}
			}
			locs.add(e2.location);
			closureDepends.put(var, mydeps);
			return new CreationOfVar(var, e2.location, "clos" + var.idx);
		} else if (expr instanceof CastExpr) {
			CastExpr ce = (CastExpr) expr;
			CreationOfVar cv = (CreationOfVar) convertValue(locs, substs, ce.expr);
			HSIEBlock closure = form.getClosure(cv.var);
			closure.downcastType = (Type) ((PackageVar)ce.castTo).defn;
			return cv;
		}
		else {
			System.out.println("HSIE Cannot Handle: " + expr);
			throw new UtilException("HSIE Cannot handle " + expr + " " + (expr != null? " of type " + expr.getClass() : ""));
		}
	}

	public List<CreationOfVar> closureDependencies(Object var) {
		List<CreationOfVar> ret = new ArrayList<CreationOfVar>();
		closeDependencies(ret, var);
		return ret;
	}

	private void closeDependencies(List<CreationOfVar> ret, Object var) {
		List<CreationOfVar> more = null;
		if (var instanceof Var)
			more = closureDepends.get(var);
		else if (var instanceof CreationOfVar)
			more = closureDepends.get(((CreationOfVar)var).var);

		if (more == null)
			return;
		
		for (CreationOfVar cv : more)
			if (!ret.contains(cv)) {
				closeDependencies(ret, cv);
				if (ret.contains(cv))
					throw new UtilException("I suspect this is a cycle");
				ret.add(cv);
			}
	}

	private static void gatherScopedVars(TreeSet<VarNestedFromOuterFunctionScope> set, RWFunctionDefinition defn) {
		for (RWFunctionCaseDefn fcd : defn.cases) {
			gatherScopedVars(set, fcd.expr);
		}
	}

	private static void gatherScopedVars(TreeSet<VarNestedFromOuterFunctionScope> set, RWHandlerImplements hi) {
		for (Object o : hi.boundVars) {
			HandlerLambda hl = (HandlerLambda)o;
			if (hl.scopedFrom != null)
				set.add(hl.scopedFrom);
		}
		for (RWMethodDefinition m : hi.methods) {
			gatherScopedVars(set, m);
		}
	}
	
	private static void gatherScopedVars(TreeSet<VarNestedFromOuterFunctionScope> set, RWMethodDefinition defn) {
		for (RWMethodCaseDefn mcd : defn.cases) {
			for (RWMethodMessage mm : mcd.messages)
				gatherScopedVars(set, mm.expr);
		}
	}
	
	private static void gatherScopedVars(TreeSet<VarNestedFromOuterFunctionScope> set, Object expr) {
		if (expr instanceof NumericLiteral || expr instanceof StringLiteral || 
			expr instanceof LocalVar || expr instanceof CardMember || expr instanceof CardFunction || expr instanceof CardStateRef || expr instanceof ObjectReference ||
			expr instanceof PackageVar || 
			expr instanceof RWStructDefn || expr instanceof RWFunctionDefinition || expr instanceof TemplateListVar)
			; // nothing to do; no recursion
		else if (expr instanceof VarNestedFromOuterFunctionScope) {
			VarNestedFromOuterFunctionScope sv = (VarNestedFromOuterFunctionScope)expr;
			set.add(sv);
		} else if (expr instanceof ApplyExpr) {
			ApplyExpr ae = (ApplyExpr) expr;
			gatherScopedVars(set, ae.fn);
			for (Object o : ae.args)
				gatherScopedVars(set, o);
		} else if (expr instanceof HandlerLambda) {
			HandlerLambda hl = (HandlerLambda) expr;
			if (hl.scopedFrom != null)
				set.add(hl.scopedFrom);
		} else if (expr instanceof CastExpr) {
			CastExpr ce = (CastExpr) expr;
			gatherScopedVars(set, ce.castTo);
			gatherScopedVars(set, ce.expr);
		} else
			throw new UtilException("Cannot handle scopedVars in " + (expr == null ? "_null expr_" : expr.getClass()));
	}
}
