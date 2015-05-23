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
import org.flasck.flas.vcode.hsieForm.Var;
import org.zinutils.exceptions.UtilException;

public class HSIE {
	public static HSIEForm handle(FunctionDefinition defn) {
		HSIEForm ret = new HSIEForm(defn.name, defn.nargs);
		MetaState ms = new MetaState(ret);
		if (defn.nargs == 0)
			return handleConstant(ms, defn);
		// build a state with the current set of variables and the list of patterns => expressions that they deal with
		ms.add(buildFundamentalState(ms, ret, defn.nargs, defn.cases));
		while (!ms.allDone()) {
			State f = ms.first();
			recurse(ms, f);
		}
		ret.dump();
		return ret;
	}

	public static HSIEForm handleExpr(Object expr) {
		MetaState ms = new MetaState(new HSIEForm("", 0));
		Object ret = ms.getValueFor(new SubstExpr(expr));
		ms.form.doReturn(ret, ms.closureDependencies(ret));
		return ms.form;
	}

	private static HSIEForm handleConstant(MetaState ms, FunctionDefinition defn) {
		if (defn.cases.size() != 1)
			throw new UtilException("Constants can only have one case");
		Object ret = ms.getValueFor(new SubstExpr(defn.cases.get(0).expr));
		ms.form.doReturn(ret, ms.closureDependencies(ret));
		return ms.form;
	}

	private static State buildFundamentalState(MetaState ms, HSIEBlock block, int nargs, List<FunctionCaseDefn> cases) {
		State s = new State(block);
		List<Var> formals = new ArrayList<Var>();
		for (int i=0;i<nargs;i++)
			formals.add(ms.allocateVar());
		Map<Object, SubstExpr> exprs = new HashMap<Object, SubstExpr>();
		for (FunctionCaseDefn c : cases) {
			SubstExpr ex = new SubstExpr(c.expr);
			createSubsts(ms, c.intro.args, formals, ex);
			exprs.put(c.expr, ex);
		}
		for (int i=0;i<nargs;i++) {
			for (FunctionCaseDefn c : cases) {
				s.associate(formals.get(i), c.intro.args.get(i), exprs.get(c.expr));
			}
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
			evalExpr(ms, s, null);
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
			Set<SubstExpr> mycases = new HashSet<SubstExpr>();
			for (NestedBinds nb : elim.ctorCases.get(ctor)) {
				if (nb.args != null) {
					for (Field f : nb.args)
						binds.add(f.field);
				}
				possibles.add(nb.substExpr);
				mycases.add(nb.substExpr);
			}
			possibles.addAll(elim.undecidedCases);
			State s1 = s.cloneEliminate(elim.var, blk, possibles);
			Map<String, Var> mapFieldNamesToVars = new TreeMap<String, Var>();
			for (String b : binds) {
				Var v = ms.varFor(elim.var, b);
				blk.bindCmd(v, elim.var, b);
				mapFieldNamesToVars.put(b, v);
			}
			boolean wantS1 = false;
			for (NestedBinds nb : elim.ctorCases.get(ctor)) {
				if (nb.ifConst != null) {
					System.out.println("Handling constant " + nb.ifConst.value);
					HSIEBlock inner = blk.ifCmd(elim.var, Integer.parseInt(nb.ifConst.value));
					State s3 = s1.duplicate(inner);
					System.out.println("---");
					s3.dump();
					System.out.println("---");
					
					addState(ms, s3, casesForConst(elim.ctorCases.get(ctor), nb.ifConst.value));
				} else {
					for (String b : binds) {
						Object patt = nb.matchField(b);
						if (patt != null) {
							s1.associate(ms.varFor(elim.var, b), patt, nb.substExpr);
						}
					}
					wantS1 = true;
				}
			}
			if (wantS1)
				addState(ms, s1, mycases);
		}
		{
			System.out.println(elim.var + " is none of the above");
			addState(ms, s.cloneEliminate(elim.var, s.writeTo, elim.undecidedCases), elim.undecidedCases);
		}
	}

	private static Set<SubstExpr> casesForConst(List<NestedBinds> nbs, String value) {
		Set<SubstExpr> possibles = new HashSet<SubstExpr>();
		for (NestedBinds nb : nbs) {
			if (nb.ifConst == null || nb.ifConst.value.equals(value))
			possibles.add(nb.substExpr);
		}
		return possibles;
	}

	private static void addState(MetaState ms, State s, Set<SubstExpr> mycases) {
		if (s.hasNeeds()) {
			System.out.println("Adding state ---");
			s.dump();
			System.out.println("---");
			ms.allStates.add(s);
		} else {
			System.out.println("Resolving to ---");
			evalExpr(ms, s, mycases);
			s.dump();
			System.out.println("---");
		}
	}

	private static void evalExpr(MetaState ms, State s, Set<SubstExpr> mycases) {
		SubstExpr e = s.singleExpr(mycases);
		System.out.println("Have expr " + e);
		if (e != null) {
			Object ret = ms.getValueFor(e);
			s.writeTo.doReturn(ret, ms.closureDependencies(ret));
		} else {
			if (s.writeTo instanceof HSIEForm)
				s.writeTo.caseError();
		}
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
						o.ifConst("Number", cp, pe.getValue());
					} else
						throw new UtilException("HSIE Cannot handle constant pattern for " + cp.type);
				} else
					System.out.println("HSIE Cannot handle pattern " + patt.getClass());
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
