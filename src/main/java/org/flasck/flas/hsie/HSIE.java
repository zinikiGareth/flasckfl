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

import org.flasck.flas.errors.ErrorResult;
import org.flasck.flas.parsedForm.ConstPattern;
import org.flasck.flas.parsedForm.ConstructorMatch;
import org.flasck.flas.parsedForm.ConstructorMatch.Field;
import org.flasck.flas.parsedForm.FunctionCaseDefn;
import org.flasck.flas.parsedForm.FunctionDefinition;
import org.flasck.flas.parsedForm.Scope;
import org.flasck.flas.parsedForm.TypedPattern;
import org.flasck.flas.parsedForm.VarPattern;
import org.flasck.flas.rewriter.Rewriter;
import org.flasck.flas.vcode.hsieForm.CreationOfVar;
import org.flasck.flas.vcode.hsieForm.HSIEBlock;
import org.flasck.flas.vcode.hsieForm.HSIEForm;
import org.flasck.flas.vcode.hsieForm.HSIEForm.CodeType;
import org.flasck.flas.vcode.hsieForm.Var;
import org.zinutils.exceptions.UtilException;
import org.zinutils.utils.StringComparator;

public class HSIE {
	private final ErrorResult errors;
	private final Rewriter rewriter;
	private int exprIdx;

	public HSIE(ErrorResult errors, Rewriter rewriter, Scope biscope) {
		this.errors = errors;
		this.rewriter = rewriter;
		exprIdx = 0;
	}
	
	public HSIEForm handle(Map<String, HSIEForm> previous, FunctionDefinition defn) {
		return handle(previous, defn, 0, new HashMap<String, CreationOfVar>());
	}
	
	public HSIEForm handle(Map<String, HSIEForm> previous, FunctionDefinition defn, int alreadyUsed, Map<String, CreationOfVar> map) {
		HSIEForm ret = new HSIEForm(defn.mytype, defn.name, alreadyUsed, map, defn.nargs);
		MetaState ms = new MetaState(rewriter, previous, ret);
		if (defn.nargs == 0)
			return handleConstant(ms, defn);
		// build a state with the current set of variables and the list of patterns => expressions that they deal with
		ms.add(buildFundamentalState(ms, ret, map, defn.nargs, defn.cases));
		try {
			while (!ms.allDone()) {
				State f = ms.first();
				recurse(ms, f);
			}
		} catch (HSIEException ex) {
			errors.message(ex.block, ex.msg);
		}
		return ret;
	}

	public HSIEForm handleExpr(Object expr, CodeType type) {
		MetaState ms = new MetaState(rewriter, new HashMap<String, HSIEForm>(), new HSIEForm(type, "", 0, new HashMap<String, CreationOfVar>(), 0));
		ms.writeExpr(new SubstExpr(expr, exprIdx++), ms.form);
//		ms.form.doReturn(ret, ms.closureDependencies(ret));
		return ms.form;
	}

	public HSIEForm handleExprWith(Object expr, CodeType type, List<String> vars) {
		HSIEForm blk = new HSIEForm(type, "_expr_", 0, new TreeMap<String, CreationOfVar>(new StringComparator()), vars.size());
		MetaState ms = new MetaState(rewriter, new HashMap<String, HSIEForm>(), blk);
//		State s = new State(blk);
//		ms.add(s);
		Map<String, CreationOfVar> map = new TreeMap<String, CreationOfVar>(new StringComparator());
		for (String v : vars) {
			map.put(v, new CreationOfVar(ms.allocateVar(), null, v));
		}
		SubstExpr se = new SubstExpr(expr, exprIdx++);
		se.alsoSub(map);
		ms.writeExpr(se, ms.form);
//		ms.form.doReturn(ret, ms.closureDependencies(ret));
		return ms.form;
	}

	private HSIEForm handleConstant(MetaState ms, FunctionDefinition defn) {
		if (defn.cases.size() != 1)
			throw new UtilException("Constants can only have one case");
		ms.writeExpr(new SubstExpr(defn.cases.get(0).expr, exprIdx++), ms.form);
//		ms.form.doReturn(ret, ms.closureDependencies(ret));
		return ms.form;
	}

	private State buildFundamentalState(MetaState ms, HSIEForm form, Map<String, CreationOfVar> map, int nargs, List<FunctionCaseDefn> cases) {
		State s = new State(form);
		List<Var> formals = new ArrayList<Var>();
		for (int i=0;i<nargs;i++)
			formals.add(ms.allocateVar());
		Map<Object, SubstExpr> exprs = new HashMap<Object, SubstExpr>();
		for (FunctionCaseDefn c : cases) {
			SubstExpr ex = new SubstExpr(c.expr, exprIdx++);
			ex.alsoSub(map);
			form.exprs.add(ex);
			createSubsts(ms, c.intro.args, formals, ex);
			exprs.put(c, ex);
		}
		for (int i=0;i<nargs;i++) {
			for (FunctionCaseDefn c : cases) {
				s.associate(formals.get(i), c.intro.args.get(i), exprs.get(c));
			}
		}
		return s;
	}

	private void createSubsts(MetaState ms, List<Object> args, List<Var> formals, SubstExpr expr) {
		for (int i=0;i<args.size();i++) {
			Object arg = args.get(i);
			if (arg instanceof VarPattern) {
				VarPattern vp = (VarPattern) arg;
				String called = vp.var;
				expr.subst(called, new CreationOfVar(formals.get(i), vp.varLoc, called));
			} else if (arg instanceof ConstructorMatch)
				ctorSub((ConstructorMatch) arg, ms, formals.get(i), expr);
			else if (arg instanceof ConstPattern)
				;
			else if (arg instanceof TypedPattern) {
				TypedPattern tp = (TypedPattern) arg;
				String called = tp.var;
				expr.subst(called, new CreationOfVar(formals.get(i), tp.varLocation, called));
			} else
				throw new UtilException("Not substituting into " + arg.getClass());
		}
	}

	private void ctorSub(ConstructorMatch cm, MetaState ms, Var from, SubstExpr expr) {
		for (Field x : cm.args) {
			Var v = ms.varFor(from, x.field);
			if (x.patt instanceof VarPattern) {
				VarPattern vp = (VarPattern)x.patt;
				String called = vp.var;
				expr.subst(called, new CreationOfVar(v, vp.varLoc, called));
			} else if (x.patt instanceof ConstructorMatch)
				ctorSub((ConstructorMatch)x.patt, ms, v, expr);
			else if (x.patt instanceof ConstPattern)
				;
			else
				throw new UtilException("Not substituting into " + x.patt.getClass());
				
		}
	}

	private void recurse(MetaState ms, State s) {
//		System.out.println("------ Entering recurse");
		Table t = buildDecisionTable(s);
//		t.dump();
		boolean needChoice = false;
		List<Option> dulls = new ArrayList<Option>();
		for (Option o : t) {
			if (o.dull()) {
//				System.out.println(o.var + " is dull");
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
//		t.dump();
		Option elim = chooseBest(t);
//		System.out.println("Choosing cases based on " + elim.var);
		s.writeTo.head(elim.var);
		CreationOfVar cv = new CreationOfVar(elim.var, null, "ev"+elim.var.idx);
		for (String ctor : elim.ctorCases) {
//			System.out.println("Choosing " + elim.var + " to match " + ctor +":");
			List<NestedBinds> list = elim.ctorCases.get(ctor);
			ms.form.dependsOn(ctor);
			HSIEBlock blk = s.writeTo.switchCmd(list.get(0).location, elim.var, ctor);
			Set<String> binds = new TreeSet<String>();
			Set<SubstExpr> possibles = new HashSet<SubstExpr>();
			Set<SubstExpr> mycases = new HashSet<SubstExpr>();
			for (NestedBinds nb : list) {
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
			for (NestedBinds nb : orderIfs(list)) {
				if (nb.ifConst != null) {
//					System.out.println("Handling constant " + nb.ifConst.value);
					HSIEBlock inner;
					if (nb.ifConst.type == ConstPattern.INTEGER)
						inner = blk.ifCmd(cv, Integer.parseInt(nb.ifConst.value));
					else if (nb.ifConst.type == ConstPattern.BOOLEAN)
						inner = blk.ifCmd(cv, Boolean.parseBoolean(nb.ifConst.value));
					else
						throw new UtilException("Cannot handle " + nb.ifConst);
					State s3 = s1.duplicate(inner);
//					System.out.println("---");
//					s3.dump();
//					System.out.println("---");
					
					addState(ms, s3, casesForConst(list, nb.ifConst.value));
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
//			System.out.println(elim.var + " is none of the above");
			addState(ms, s.cloneEliminate(elim.var, s.writeTo, elim.undecidedCases), elim.undecidedCases);
		}
	}

	private List<NestedBinds> orderIfs(List<NestedBinds> list) {
		List<NestedBinds> ret = new ArrayList<NestedBinds>();
		for (NestedBinds nb : list) {
			if (nb.ifConst == null)
				ret.add(0, nb);
			else {
				boolean done = false;
				for (int j=0;!done && j<ret.size();j++) {
					NestedBinds other = ret.get(j);
					if (other.ifConst == null)
						continue;
					else if (nb.ifConst.type < other.ifConst.type || (nb.ifConst.type == other.ifConst.type && nb.ifConst.value.compareTo(other.ifConst.value) <= 0)) {
						ret.add(j, nb);
						done = true;
					}
				}
				if (!done)
					ret.add(nb);
			}
		}
		return ret;
	}

	private Set<SubstExpr> casesForConst(List<NestedBinds> nbs, String value) {
		Set<SubstExpr> possibles = new HashSet<SubstExpr>();
		for (NestedBinds nb : nbs) {
			if (nb.ifConst == null || nb.ifConst.value.equals(value))
				possibles.add(nb.substExpr);
		}
		return possibles;
	}

	private void addState(MetaState ms, State s, Set<SubstExpr> mycases) {
		if (s.hasNeeds()) {
//			System.out.println("Adding state ---");
//			s.dump();
//			System.out.println("---");
			ms.allStates.add(s);
		} else {
//			System.out.println("Resolving to ---");
//			s.dump();
//			System.out.println("---");
			evalExpr(ms, s, mycases);
		}
	}

	private void evalExpr(MetaState ms, State s, Set<SubstExpr> mycases) {
		SubstExpr e = s.singleExpr(mycases);
//		System.out.println("Have expr " + e);
		if (e != null) {
			ms.writeExpr(e, s.writeTo);
		} else {
			if (s.writeTo instanceof HSIEForm)
				s.writeTo.caseError();
		}
	}

	private Table buildDecisionTable(State s) {
		Table t = new Table();
		for (Entry<Var, PattExpr> e : s) {
			Option o = t.createOption(e.getKey());
			for (Entry<Object, SubstExpr> pe : e.getValue()) {
				Object patt = pe.getKey();
				if (patt instanceof VarPattern) {
					o.anything(pe.getValue(), ((VarPattern)patt).var);
				} else if (patt instanceof ConstructorMatch) {
					ConstructorMatch cm = (ConstructorMatch) patt;
					o.ifCtor(cm.location, cm.ref.uniqueName(), cm.args, pe.getValue());
				} else if (patt instanceof TypedPattern) {
					TypedPattern tp = (TypedPattern) patt;
					o.ifCtor(tp.typeLocation, tp.type.name(), new ArrayList<Field>(), pe.getValue());
				} else if (patt instanceof ConstPattern) {
					ConstPattern cp = (ConstPattern) patt;
					if (cp.type == ConstPattern.INTEGER) {
						o.ifConst("Number", cp, pe.getValue());
					} else if (cp.type == ConstPattern.BOOLEAN) {
						o.ifConst("Boolean", cp, pe.getValue());
					} else
						throw new UtilException("HSIE Cannot handle constant pattern for " + cp.type);
				} else
					throw new UtilException("HSIE Cannot handle pattern " + patt.getClass());
			}
		}
		return t;
	}

	private Option chooseBest(Table t) {
		Option best = null;
		for (Option o : t) {
			if (o.dull())
				continue;
			if (best == null || o.valuation() < best.valuation())
				best = o;
		}
		return best;
	}
}
