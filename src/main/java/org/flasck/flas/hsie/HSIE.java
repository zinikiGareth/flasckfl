package org.flasck.flas.hsie;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import org.flasck.flas.commonBase.ConstPattern;
import org.flasck.flas.errors.ErrorResult;
import org.flasck.flas.rewriter.Rewriter;
import org.flasck.flas.rewrittenForm.RWConstructorMatch;
import org.flasck.flas.rewrittenForm.RWConstructorMatch.Field;
import org.flasck.flas.rewrittenForm.RWFunctionCaseDefn;
import org.flasck.flas.rewrittenForm.RWFunctionDefinition;
import org.flasck.flas.rewrittenForm.RWTypedPattern;
import org.flasck.flas.rewrittenForm.RWVarPattern;
import org.flasck.flas.vcode.hsieForm.CreationOfVar;
import org.flasck.flas.vcode.hsieForm.HSIEBlock;
import org.flasck.flas.vcode.hsieForm.HSIEForm;
import org.flasck.flas.vcode.hsieForm.Var;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.zinutils.exceptions.UtilException;
import org.zinutils.graphs.Node;
import org.zinutils.graphs.Orchard;
import org.zinutils.graphs.Tree;
import org.zinutils.utils.StringComparator;

public class HSIE {
	static Logger logger = LoggerFactory.getLogger("HSIE");
	private final ErrorResult errors;
	private final Rewriter rewriter;
	private final Map<String, HSIEForm> forms = new TreeMap<String, HSIEForm>(new StringComparator());
	private int exprIdx;

	public HSIE(ErrorResult errors, Rewriter rewriter) {
		this.errors = errors;
		this.rewriter = rewriter;
		exprIdx = 0;
	}
	
	public Set<HSIEForm> orchard(Orchard<RWFunctionDefinition> orch) {
		VarFactory vf = new VarFactory();
		TreeMap<String, HSIEForm> ret = new TreeMap<String, HSIEForm>();
		for (RWFunctionDefinition fn : orch.allNodes()) {
			HSIEForm hf = new HSIEForm(fn.location, fn.name(), fn.nargs(), fn.mytype, vf);
			forms.put(fn.name, hf);
			ret.put(fn.name, hf);
		}
		GatherExternals ge = new GatherExternals(ret);
		for (RWFunctionDefinition fn : orch.allNodes())
			ge.process(fn);
		logger.info("HSIE transforming orchard in parallel: " + orch);
		for (Tree<RWFunctionDefinition> t : orch) {
			hsieTree(t, t.getRoot());
		}
		return new TreeSet<HSIEForm>(ret.values());
	}

	private void hsieTree(Tree<RWFunctionDefinition> t, Node<RWFunctionDefinition> node) {
		logger.info("HSIE transforming " + node.getEntry().name());
		handle(node.getEntry());
		for (Node<RWFunctionDefinition> x : t.getChildren(node))
			hsieTree(t, x);
	}

	private void handle(RWFunctionDefinition defn) {
		HSIEForm ret = forms.get(defn.name);
		if (ret == null)
			throw new UtilException("There is no form for " + defn.name);
		MetaState ms = new MetaState(errors, rewriter, forms, ret);
		if (defn.nargs() == 0) {
			handleConstant(ms, defn);
			return;
		}
		// build a state with the current set of variables and the list of patterns => expressions that they deal with
		ms.add(buildFundamentalState(ms, ret, defn.nargs(), defn.cases));
		try {
			while (!ms.allDone()) {
				State f = ms.first();
				recurse(ms, f);
			}
		} catch (HSIEException ex) {
			errors.message(ex.block, ex.msg);
		}
	}

	private HSIEForm handleConstant(MetaState ms, RWFunctionDefinition defn) {
		if (defn.cases.size() != 1)
			throw new UtilException("Constants can only have one case");
		ms.writeExpr(new SubstExpr(defn.cases.get(0).expr, exprIdx++), ms.form);
//		ms.form.doReturn(ret, ms.closureDependencies(ret));
		return ms.form;
	}

	private State buildFundamentalState(MetaState ms, HSIEForm form, int nargs, List<RWFunctionCaseDefn> cases) {
		State s = new State(form);
		List<Var> formals = new ArrayList<Var>();
		for (int i=0;i<nargs;i++)
			formals.add(ms.allocateVar());
		Map<Object, SubstExpr> exprs = new HashMap<Object, SubstExpr>();
		for (RWFunctionCaseDefn c : cases) {
			SubstExpr ex = new SubstExpr(c.expr, exprIdx++);
			createSubsts(ms, c.caseName(), c.args(), formals, ex);
			exprs.put(c, ex);
		}
		for (int i=0;i<nargs;i++) {
			for (RWFunctionCaseDefn c : cases) {
				s.associate(formals.get(i), c.args().get(i), exprs.get(c));
			}
		}
		return s;
	}

	private void createSubsts(MetaState ms, String methName, List<Object> args, List<Var> formals, SubstExpr expr) {
		for (int i=0;i<args.size();i++) {
			Object arg = args.get(i);
			if (arg instanceof RWVarPattern) {
				RWVarPattern vp = (RWVarPattern) arg;
				String called = vp.var;
				expr.subst(called, new CreationOfVar(formals.get(i), vp.varLoc, called));
			} else if (arg instanceof RWConstructorMatch)
				ctorSub((RWConstructorMatch) arg, ms, formals.get(i), expr);
			else if (arg instanceof ConstPattern)
				;
			else if (arg instanceof RWTypedPattern) {
				RWTypedPattern tp = (RWTypedPattern) arg;
				String called = tp.var;
				expr.subst(called, new CreationOfVar(formals.get(i), tp.varLocation, called));
			} else
				throw new UtilException("Not substituting into " + arg.getClass());
		}
	}

	private void ctorSub(RWConstructorMatch cm, MetaState ms, Var from, SubstExpr expr) {
		for (Field x : cm.args) {
			Var v = ms.varFor(from, x.field);
			if (x.patt instanceof RWVarPattern) {
				RWVarPattern vp = (RWVarPattern)x.patt;
				String called = vp.var;
				expr.subst(called, new CreationOfVar(v, vp.varLoc, called));
			} else if (x.patt instanceof RWConstructorMatch)
				ctorSub((RWConstructorMatch)x.patt, ms, v, expr);
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
		s.writeTo.head(elim.location, elim.var);
		CreationOfVar cv = new CreationOfVar(elim.var, null, "ev"+elim.var.idx);
		for (String ctor : elim.ctorCases) {
//			System.out.println("Choosing " + elim.var + " to match " + ctor +":");
			List<NestedBinds> list = elim.ctorCases.get(ctor);
//			ms.form.dependsOn(ctor);
			HSIEBlock blk = s.writeTo.switchCmd(NestedBinds.firstLocation(list), elim.var, ctor);
			Set<Field> binds = new TreeSet<Field>();
			Set<SubstExpr> possibles = new HashSet<SubstExpr>();
			Set<SubstExpr> mycases = new HashSet<SubstExpr>();
			for (NestedBinds nb : list) {
				if (nb.args != null) {
					for (Field f : nb.args)
						binds.add(f);
				}
				possibles.add(nb.substExpr);
				mycases.add(nb.substExpr);
			}
			possibles.addAll(elim.undecidedCases);
			State s1 = s.cloneEliminate(elim.var, blk, possibles);
			Map<String, Var> mapFieldNamesToVars = new TreeMap<String, Var>();
			for (Field b : binds) {
				Var v = ms.varFor(elim.var, b.field);
				blk.bindCmd(b.location, v, elim.var, b.field);
				mapFieldNamesToVars.put(b.field, v);
			}
			boolean wantS1 = false;
			for (NestedBinds nb : orderIfs(list)) {
				if (nb.ifConst != null) {
//					System.out.println("Handling constant " + nb.ifConst.value);
					HSIEBlock inner;
					if (nb.ifConst.type == ConstPattern.INTEGER)
						inner = blk.ifCmd(nb.location, cv, Integer.parseInt(nb.ifConst.value));
					else if (nb.ifConst.type == ConstPattern.BOOLEAN)
						inner = blk.ifCmd(nb.location, cv, Boolean.parseBoolean(nb.ifConst.value));
					else
						throw new UtilException("Cannot handle " + nb.ifConst);
					State s3 = s1.duplicate(inner);
//					System.out.println("---");
//					s3.dump();
//					System.out.println("---");
					
					addState(ms, s3, casesForConst(list, nb.ifConst.value));
				} else {
					for (Field b : binds) {
						Object patt = nb.matchField(b.field);
						if (patt != null) {
							s1.associate(ms.varFor(elim.var, b.field), patt, nb.substExpr);
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
			Option o = t.createOption(e.getValue().firstLocation(), e.getKey());
			for (Entry<Object, SubstExpr> pe : e.getValue()) {
				Object patt = pe.getKey();
				if (patt instanceof RWVarPattern) {
					o.anything(pe.getValue(), ((RWVarPattern)patt).var);
				} else if (patt instanceof RWConstructorMatch) {
					RWConstructorMatch cm = (RWConstructorMatch) patt;
					o.ifCtor(cm.location, cm.ref.uniqueName(), cm.args, pe.getValue());
				} else if (patt instanceof RWTypedPattern) {
					RWTypedPattern tp = (RWTypedPattern) patt;
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

	public Collection<HSIEForm> allForms() {
		return forms.values();
	}

	public HSIEForm getForm(String name) {
		return forms.get(name);
	}
}
