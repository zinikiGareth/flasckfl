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
import org.flasck.flas.rewrittenForm.RWConstructorMatch;
import org.flasck.flas.rewrittenForm.RWConstructorMatch.Field;
import org.flasck.flas.rewrittenForm.RWFunctionCaseDefn;
import org.flasck.flas.rewrittenForm.RWFunctionDefinition;
import org.flasck.flas.rewrittenForm.RWTypedPattern;
import org.flasck.flas.rewrittenForm.RWVarPattern;
import org.flasck.flas.vcode.hsieForm.VarInSource;
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
	private final Map<String, HSIEForm> forms = new TreeMap<String, HSIEForm>(new StringComparator());

	public HSIE(ErrorResult errors) {
		this.errors = errors;
	}
	
	public void createForms(Orchard<RWFunctionDefinition> orch) {
		VarFactory vf = new VarFactory();
		GatherExternals ge = new GatherExternals();
		for (RWFunctionDefinition fn : orch.allNodes()) {
			HSIEForm hf = new HSIEForm(fn.location, fn.name(), fn.nargs(), fn.mytype, vf);
			ge.process(hf, fn);
			forms.put(fn.name, hf);
		}
	}
	
	public Set<HSIEForm> orchard(Orchard<RWFunctionDefinition> orch) {
		TreeMap<String, HSIEForm> ret = new TreeMap<String, HSIEForm>();
		for (RWFunctionDefinition fn : orch.allNodes()) {
			ret.put(fn.name, forms.get(fn.name));
		}
		GatherExternals.transitiveClosure(forms, ret.values());
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
		MetaState ms = new MetaState(ret);
		GenerateClosures gc = new GenerateClosures(errors, ms, forms, ret);
		if (defn.nargs() == 0) {
			if (defn.cases.size() != 1)
				throw new UtilException("Constants can only have one case");
			ms.addExpr(defn.cases.get(0).expr);
			gc.generateExprClosures();
			ms.writeExpr(ms.form, 0);
		} else {
			// build a state with the current set of variables and the list of patterns => expressions that they deal with
			ms.add(buildFundamentalState(ms, ret, defn.nargs(), defn.cases));
			gc.generateScopingClosures();
			gc.generateExprClosures();
			try {
				while (!ms.allDone()) {
					State f = ms.first();
					recurse(ms, f);
				}
			} catch (HSIEException ex) {
				errors.message(ex.where, ex.msg);
			}
		}
	}

	private State buildFundamentalState(MetaState ms, HSIEForm form, int nargs, List<RWFunctionCaseDefn> cases) {
		State s = new State(form);
		List<Var> formals = new ArrayList<Var>();
		for (int i=0;i<nargs;i++)
			formals.add(ms.allocateVar());
		Map<Object, Integer> exprs = new HashMap<Object, Integer>();
		for (RWFunctionCaseDefn c : cases) {
			createSubsts(ms, c.caseName(), c.args(), formals, c.expr);
			exprs.put(c, ms.exprs().size());
			ms.addExpr(c.expr);
		}
		for (int i=0;i<nargs;i++) {
			for (RWFunctionCaseDefn c : cases) {
				s.associate(formals.get(i), c.args().get(i), exprs.get(c));
			}
		}
		return s;
	}

	private void createSubsts(MetaState ms, String methName, List<Object> args, List<Var> formals, Object expr) {
		for (int i=0;i<args.size();i++) {
			Object arg = args.get(i);
			if (arg instanceof RWVarPattern) {
				RWVarPattern vp = (RWVarPattern) arg;
				String called = vp.var;
				ms.subst(called, new VarInSource(formals.get(i), vp.varLoc, called));
			} else if (arg instanceof RWConstructorMatch)
				ctorSub((RWConstructorMatch) arg, ms, formals.get(i), expr);
			else if (arg instanceof ConstPattern)
				;
			else if (arg instanceof RWTypedPattern) {
				RWTypedPattern tp = (RWTypedPattern) arg;
				String called = tp.var;
				ms.subst(called, new VarInSource(formals.get(i), tp.varLocation, called));
			} else
				throw new UtilException("Not substituting into " + arg.getClass());
		}
	}

	private void ctorSub(RWConstructorMatch cm, MetaState ms, Var from, Object expr) {
		for (Field x : cm.args) {
			Var v = ms.varFor(from, x.field);
			if (x.patt instanceof RWVarPattern) {
				RWVarPattern vp = (RWVarPattern)x.patt;
				String called = vp.var;
				ms.subst(called, new VarInSource(v, vp.varLoc, called));
			} else if (x.patt instanceof RWConstructorMatch)
				ctorSub((RWConstructorMatch)x.patt, ms, v, expr);
			else if (x.patt instanceof ConstPattern)
				;
			else
				throw new UtilException("Not substituting into " + x.patt.getClass());
				
		}
	}

	private void recurse(MetaState ms, State s) {
		logger.info("Recursing with state " + s);
//		System.out.println("------ Entering recurse");
		Table t = buildDecisionTable(s);
		t.dump(logger);
		boolean needChoice = false;
		List<Option> dulls = new ArrayList<Option>();
		for (Option o : t) {
			if (o.dull()) {
				logger.info("Eliminating var " + o.var + " which does not reduce choice");
				s.eliminate(o.var);
				dulls.add(o);
			} else
				needChoice = true;
		}
		for (Option o : dulls)
			t.remove(o);
		if (!needChoice) {
			logger.info("There is no choice remaining: " + s);
			evalExpr(ms, s, null);
			return;
		}
//		t.dump();
		Option elim = chooseBest(t);
		logger.info("Decided that best switching var is " + elim.var);
		s.writeTo.head(elim.location, elim.var);
		VarInSource cv = new VarInSource(elim.var, null, "ev"+elim.var.idx);
		for (String ctor : elim.ctorCases) {
//			System.out.println("Choosing " + elim.var + " to match " + ctor +":");
			List<NestedBinds> list = elim.ctorCases.get(ctor);
//			ms.form.dependsOn(ctor);
			HSIEBlock blk = s.writeTo.switchCmd(NestedBinds.firstLocation(list), elim.var, ctor);
			Set<Field> binds = new TreeSet<Field>();
			Set<Integer> possibles = new HashSet<>();
			Set<Integer> mycases = new HashSet<>();
			for (NestedBinds nb : list) {
				if (nb.args != null) {
					for (Field f : nb.args)
						binds.add(f);
				}
				possibles.add(nb.expr);
				mycases.add(nb.expr);
			}
			logger.info("Matching cases = " + mycases);
			logger.info("Undecided cases = " + elim.undecidedCases);
			possibles.addAll(elim.undecidedCases);
			State s1 = s.cloneEliminate(elim.var, blk, possibles);
			logger.info("After selecting " + elim.var + " as " + ctor + ", state is " + s1);
			Map<String, Var> mapFieldNamesToVars = new TreeMap<String, Var>();
			for (Field b : binds) {
				Var v = ms.varFor(elim.var, b.field);
				blk.bindCmd(b.location, v, elim.var, b.field);
				mapFieldNamesToVars.put(b.field, v);
			}
			boolean wantS1 = false;
			for (NestedBinds nb : orderIfs(list)) {
				if (nb.ifConst != null) {
					logger.info("Handling constant " + nb.ifConst.value);
					HSIEBlock inner;
					if (nb.ifConst.type == ConstPattern.INTEGER)
						inner = blk.ifCmd(nb.location, cv, Integer.parseInt(nb.ifConst.value));
					else if (nb.ifConst.type == ConstPattern.BOOLEAN)
						inner = blk.ifCmd(nb.location, cv, Boolean.parseBoolean(nb.ifConst.value));
					else
						throw new UtilException("Cannot handle " + nb.ifConst);
					State s3 = s1.duplicate(inner);
					addState(ms, s3, casesForConst(list, nb.ifConst.value));
				} else {
					for (Field b : binds) {
						Object patt = nb.matchField(b.field);
						if (patt != null) {
							s1.associate(ms.varFor(elim.var, b.field), patt, nb.expr);
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
			State elimState = s.cloneEliminate(elim.var, s.writeTo, elim.undecidedCases);
			logger.info("After eliminating " + elim.var + ", state is " + elimState);
			addState(ms, elimState, elim.undecidedCases);
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

	private Set<Integer> casesForConst(List<NestedBinds> nbs, String value) {
		Set<Integer> possibles = new HashSet<>();
		for (NestedBinds nb : nbs) {
			if (nb.ifConst == null || nb.ifConst.value.equals(value))
				possibles.add(nb.expr);
		}
		return possibles;
	}

	private void addState(MetaState ms, State s, Set<Integer> mycases) {
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

	private void evalExpr(MetaState ms, State s, Set<Integer> mycases) {
		Integer e = s.singleExpr(mycases);
//		System.out.println("Have expr " + e);
		if (e != null) {
			ms.writeExpr(s.writeTo, e);
		} else {
			if (s.writeTo instanceof HSIEForm)
				s.writeTo.caseError();
		}
	}

	private Table buildDecisionTable(State s) {
		Table t = new Table();
		for (Entry<Var, PattExpr> e : s) {
			Option o = t.createOption(e.getValue().firstLocation(), e.getKey());
			for (Entry<Object, Integer> pe : e.getValue()) {
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
