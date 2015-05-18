package org.flasck.flas.hsie;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import org.flasck.flas.parsedForm.ConstPattern;
import org.flasck.flas.parsedForm.ConstructorMatch;
import org.flasck.flas.parsedForm.ConstructorMatch.Field;
import org.flasck.flas.parsedForm.ContainsScope;
import org.flasck.flas.parsedForm.FunctionCaseDefn;
import org.flasck.flas.parsedForm.FunctionDefinition;
import org.flasck.flas.parsedForm.Scope;
import org.flasck.flas.parsedForm.VarPattern;
import org.flasck.flas.vcode.hsieForm.HSIEBlock;
import org.flasck.flas.vcode.hsieForm.HSIEForm;
import org.flasck.flas.vcode.hsieForm.HSIEForm.Var;
import org.zinutils.exceptions.UtilException;

public class HSIE {
	public static HSIEForm handle(FunctionDefinition defn) {
		HSIEForm ret = new HSIEForm(defn.nargs);
		MetaState ms = new MetaState(ret);
		// build a state with the current set of variables and the list of patterns => expressions that they deal with
		ms.add(buildFundamentalState(ms, ret, defn.nargs, defn.cases));
		while (!ms.allDone()) {
			State f = ms.first();
			recurse(ms, f);
		}
		ret.dump();
		return ret;
	}

	private static State buildFundamentalState(MetaState ms, HSIEBlock block, int nargs, List<FunctionCaseDefn> cases) {
		State s = new State(block);
		List<Var> formals = new ArrayList<Var>();
		for (int i=0;i<nargs;i++)
			formals.add(ms.allocateVar());
		Map<Object, SubstExpr> exprs = new HashMap<Object, SubstExpr>();
		for (FunctionCaseDefn c : cases) {
			SubstExpr ex = new SubstExpr(c.expr);
			createSubsts(ms, c.args, formals, ex);
			exprs.put(c.expr, ex);
		}
		for (int i=0;i<nargs;i++) {
			for (FunctionCaseDefn c : cases)
				s.associate(formals.get(i), c.args.get(i), exprs.get(c.expr));
		}
		return s;
	}

	private static void createSubsts(MetaState ms, List<Object> args, List<Var> formals, SubstExpr expr) {
		for (int i=0;i<args.size();i++) {
			Object arg = args.get(i);
			if (arg instanceof VarPattern)
				expr.subst(((VarPattern)arg).var, formals.get(i));
			else if (arg instanceof ConstructorMatch)
				ctorSub((ConstructorMatch) arg, ms, formals.get(i), expr);
			else
				System.out.println("Not substituting into " + arg.getClass());
		}
	}

	private static void ctorSub(ConstructorMatch cm, MetaState ms, Var from, SubstExpr expr) {
		for (Field x : cm.args) {
			Var v = ms.varFor(from, x.field);
			if (x.patt instanceof VarPattern)
				expr.subst(((VarPattern)x.patt).var, v);
			else if (x.patt instanceof ConstructorMatch)
				ctorSub((ConstructorMatch)x.patt, ms, v, expr);
			else
				System.out.println("Not substituting into " + x.patt.getClass());
				
		}
	}

	private static void recurse(MetaState ms, State s) {
		System.out.println("------ Entering recurse");
		Table t = buildDecisionTable(s);
		boolean needChoice = false;
		List<Option> dulls = new ArrayList<Option>();
		for (Option o : t) {
			if (o.dull()) {
				System.out.println(o.var + " is dull");
				s.eliminate(o.var);
				dulls.add(o);
			} else
				needChoice = true;
		}
		for (Option o : dulls)
			t.remove(o);
		if (!needChoice) {
			evalExpr(s);
			return;
		}
		t.dump();
		Option elim = chooseBest(t);
		System.out.println("Switching on " + elim.var);
		s.writeTo.head(elim.var);
		for (String ctor : elim.ctorCases) {
			System.out.println("Choosing " + elim.var + " to match " + ctor +":");
			HSIEBlock blk = s.writeTo.switchCmd(elim.var, ctor);
			Set<String> binds = new TreeSet<String>();
			Set<SubstExpr> possibles = new HashSet<SubstExpr>();
			for (NestedBinds nb : elim.ctorCases.get(ctor)) {
				for (Field f : nb.args)
					binds.add(f.field);
				possibles.add(nb.substExpr);
			}
			State s1 = s.cloneEliminate(elim.var, blk, possibles);
			Map<String, Var> mapFieldNamesToVars = new TreeMap<String, Var>();
			for (String b : binds) {
				Var v = ms.varFor(elim.var, b);
				blk.bindCmd(v, elim.var, b);
				mapFieldNamesToVars.put(b, v);
			}
			for (NestedBinds nb : elim.ctorCases.get(ctor)) {
				for (String b : binds) {
					Object patt = nb.matchField(b);
					if (patt != null) {
						s1.associate(ms.varFor(elim.var, b), patt, nb.substExpr);
					}
				}
			}
			if (s1.hasNeeds()) {
				System.out.println("Adding state ---");
				s1.dump();
				System.out.println("---");
				ms.allStates.add(s1);
			} else {
				System.out.println("Resolving to ---");
				evalExpr(s1);
				s1.dump();
				System.out.println("---");
			}
		}
		{
			System.out.println(elim.var + " is none of the above");
			State s2 = s.cloneEliminate(elim.var, s.writeTo, elim.undecidedCases);
			if (s2.hasNeeds()) {
				System.out.println("Adding default state ---");
				s2.dump();
				System.out.println("---");
				ms.allStates.add(s2);
			} else {
				System.out.println("Default resolution ---");
				evalExpr(s2);
				System.out.println("---");
			}
		}
	}

	private static void evalExpr(State s) {
		SubstExpr e = s.singleExpr();
		System.out.println("Have expr " + e);
		if (s.writeTo instanceof HSIEForm)
			s.writeTo.caseError();
		else
			s.writeTo.doReturn("FallThrough");
	}

	private static Table buildDecisionTable(State s) {
		Table t = new Table();
		for (Entry<Var, PattExpr> e : s) {
			Option o = t.createOption(e.getKey());
			for (Entry<Object, SubstExpr> pe : e.getValue()) {
				Object patt = pe.getKey();
				if (patt instanceof VarPattern) {
					o.anything(pe.getValue(), ((VarPattern)patt).var);
				} else if (patt instanceof ConstructorMatch) {
					ConstructorMatch cm = (ConstructorMatch) patt;
					o.ifCtor(cm.ctor, cm.args, pe.getValue());
				} else if (patt instanceof ConstPattern) {
					ConstPattern cp = (ConstPattern) patt;
					if (cp.type == ConstPattern.INTEGER) {
						o.ifCtor("Number", new ArrayList<Field>(), pe.getValue()); // somewhere we need to attach an "IF" but that's for later
					} else
						throw new UtilException("Cannot handle constant pattern for " + cp.type);
				} else
					System.out.println("Cannot handle pattern " + patt.getClass());
			}
		}
		return t;
	}

	private static Option chooseBest(Table t) {
		Option best = null;
		for (Option o : t) {
			if (o.dull())
				continue;
			if (best == null || o.valuation() < best.valuation())
				best = o;
		}
		return best;
	}

	public static void applyTo(Scope s) {
		for (Entry<String, Object> x : s) {
			if (x instanceof FunctionDefinition) {
				handle((FunctionDefinition) x);
			}
			if (x instanceof ContainsScope)
				applyTo(((ContainsScope)x).innerScope());
		}
	}

}
