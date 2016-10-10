package org.flasck.flas.hsie;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;

import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.parsedForm.ApplyExpr;
import org.flasck.flas.parsedForm.CardFunction;
import org.flasck.flas.parsedForm.CardMember;
import org.flasck.flas.parsedForm.CardStateRef;
import org.flasck.flas.parsedForm.CastExpr;
import org.flasck.flas.parsedForm.ExternalRef;
import org.flasck.flas.parsedForm.FunctionCaseDefn;
import org.flasck.flas.parsedForm.FunctionDefinition;
import org.flasck.flas.parsedForm.FunctionLiteral;
import org.flasck.flas.parsedForm.HandlerImplements;
import org.flasck.flas.parsedForm.HandlerLambda;
import org.flasck.flas.parsedForm.IfExpr;
import org.flasck.flas.parsedForm.IterVar;
import org.flasck.flas.parsedForm.LetExpr;
import org.flasck.flas.parsedForm.LocalVar;
import org.flasck.flas.parsedForm.MethodCaseDefn;
import org.flasck.flas.parsedForm.MethodDefinition;
import org.flasck.flas.parsedForm.MethodMessage;
import org.flasck.flas.parsedForm.NumericLiteral;
import org.flasck.flas.parsedForm.ObjectReference;
import org.flasck.flas.parsedForm.PackageVar;
import org.flasck.flas.parsedForm.StringLiteral;
import org.flasck.flas.parsedForm.TemplateListVar;
import org.flasck.flas.rewriter.Rewriter;
import org.flasck.flas.rewrittenForm.ScopedVar;
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
	private final Logger logger = LoggerFactory.getLogger("HSIE");

	public class TrailItem {
		private ClosureCmd closure;
		private TreeSet<ScopedVar> avars;
		private List<CreationOfVar> depends;

		public TrailItem(List<CreationOfVar> depends, ClosureCmd closure, TreeSet<ScopedVar> avars) {
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
		logger.info("Handling " + form.fnName + "; expr = " + expr);
		// First handle the explicit "if" and "let" cases
		if (expr instanceof IfExpr) {
			IfExpr ae = (IfExpr) expr;
			List<InputPosition> elocs = new ArrayList<InputPosition>();
			HSIEBlock ifCmd = writeTo.ifCmd((CreationOfVar) convertValue(elocs, substs, ae.guard));
			writeIfExpr(substs, ae.ifExpr, ifCmd);
			if (ae.elseExpr != null)
				writeIfExpr(substs, ae.elseExpr, writeTo);
			else
				writeTo.caseError();
			return;
		} else if (expr instanceof LetExpr) {
			LetExpr let = (LetExpr) expr;
			LocatedObject lo = getValueFor(substs, let.val);
			CreationOfVar var;
			if (lo.obj instanceof CreationOfVar) {
				var = (CreationOfVar) lo.obj;
				var = new CreationOfVar(var.var, lo.loc, let.var);
			} else {
				Var v = allocateVar();
				var = new CreationOfVar(v, null, let.var);
				HSIEBlock closure = form.closure(v);
				closure.push(lo.loc, lo.obj);
			}
			substs.put(let.var, var);
			writeIfExpr(substs, let.expr, writeTo);
			return;
		}

		// Now handle scoping by resolving the vars that are included in scope 
		List<TrailItem> tis = new ArrayList<TrailItem>();
		TreeSet<ScopedVar> set = new TreeSet<ScopedVar>();
		gatherScopedVars(set, expr);
		
		logger.info(form.fnName + " claims to have " + set + " scoped vars");
		
		// Transitively close the set
		TreeSet<ScopedVar> newOnes = new TreeSet<ScopedVar>(set);
		while (!newOnes.isEmpty()) {
			TreeSet<ScopedVar> discovered = new TreeSet<ScopedVar>();
			for (ScopedVar sv : newOnes) {
				TreeSet<ScopedVar> avars = new TreeSet<ScopedVar>();
				if (sv.defn instanceof LocalVar)
					continue;
				else if (sv.defn instanceof FunctionDefinition) {
					gatherScopedVars(avars, rewriter.functions.get(sv.id));
				} else if (sv.defn instanceof MethodDefinition) {
					gatherScopedVars(avars, rewriter.standalone.get(sv.id).method);
				} else if (sv.defn instanceof HandlerImplements) {
					gatherScopedVars(avars, rewriter.callbackHandlers.get(sv.id));
				} else
					throw new UtilException("Not handling " + sv.id + " of class " + sv.defn.getClass());
				for (ScopedVar o : avars)
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
		for (ScopedVar sv : set) {
			if (sv.defn instanceof LocalVar) {
				logger.info("Ignoring " + sv.id + " which presumably should be in " + substs.keySet());
				continue;
			}
			if (!definedLocally(sv)) {
				logger.info("!!" + sv.id + " not defined locally to " + form.fnName);
				continue;
			}
			Var cv = form.allocateVar();
			ClosureCmd closure = form.closure(cv);
			TreeSet<ScopedVar> avars = new TreeSet<ScopedVar>();
			if (sv.defn instanceof MethodDefinition) {
				closure.push(sv.location, new PackageVar(sv.location, sv.id, sv.defn));
				gatherScopedVars(avars, rewriter.standalone.get(sv.id).method);
				closure.justScoping = true;
			} else if (sv.defn instanceof FunctionDefinition) {
				closure.push(sv.location, new PackageVar(sv.location, sv.id, sv.defn));
				gatherScopedVars(avars, rewriter.functions.get(sv.id));
				closure.justScoping = true;
			} else if (sv.defn instanceof HandlerImplements) {
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
			for (ScopedVar av : ti.avars) {
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

	private boolean definedLocally(ScopedVar sv) {
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
			String var = ((LocalVar)expr).var;
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
			PackageVar pv = (PackageVar)expr;
			locs.add(pv.location);
			form.dependsOn(pv);
			return expr;
		} else if (expr instanceof ScopedVar) {
			ScopedVar sv = (ScopedVar)expr;
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
			HSIEBlock closure = form.closure(var);
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

	private static void gatherScopedVars(TreeSet<ScopedVar> set, FunctionDefinition defn) {
		for (FunctionCaseDefn fcd : defn.cases) {
			gatherScopedVars(set, fcd.expr);
		}
	}

	private static void gatherScopedVars(TreeSet<ScopedVar> set, HandlerImplements hi) {
		for (Object o : hi.boundVars) {
			HandlerLambda hl = (HandlerLambda)o;
			if (hl.scopedFrom != null)
				set.add(hl.scopedFrom);
		}
		for (MethodDefinition m : hi.methods) {
			gatherScopedVars(set, m);
		}
	}
	
	private static void gatherScopedVars(TreeSet<ScopedVar> set, MethodDefinition defn) {
		for (MethodCaseDefn mcd : defn.cases) {
			for (MethodMessage mm : mcd.messages)
				gatherScopedVars(set, mm.expr);
		}
	}
	
	private static void gatherScopedVars(TreeSet<ScopedVar> set, Object expr) {
		if (expr instanceof ScopedVar) {
			ScopedVar sv = (ScopedVar)expr;
			set.add(sv);
		} else if (expr instanceof ApplyExpr) {
			ApplyExpr ae = (ApplyExpr) expr;
			gatherScopedVars(set, ae.fn);
			for (Object o : ae.args)
				gatherScopedVars(set, o);
		}
	}
}
