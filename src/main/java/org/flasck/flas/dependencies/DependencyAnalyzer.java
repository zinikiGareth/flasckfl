package org.flasck.flas.dependencies;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import org.flasck.flas.commonBase.ApplyExpr;
import org.flasck.flas.commonBase.LetExpr;
import org.flasck.flas.commonBase.NumericLiteral;
import org.flasck.flas.commonBase.StringLiteral;
import org.flasck.flas.commonBase.template.TemplateListVar;
import org.flasck.flas.errors.ErrorResult;
import org.flasck.flas.rewrittenForm.CardFunction;
import org.flasck.flas.rewrittenForm.CardMember;
import org.flasck.flas.rewrittenForm.ExternalRef;
import org.flasck.flas.rewrittenForm.FunctionLiteral;
import org.flasck.flas.rewrittenForm.IterVar;
import org.flasck.flas.rewrittenForm.LocalVar;
import org.flasck.flas.rewrittenForm.ObjectReference;
import org.flasck.flas.rewrittenForm.PackageVar;
import org.flasck.flas.rewrittenForm.RWFunctionCaseDefn;
import org.flasck.flas.rewrittenForm.RWFunctionDefinition;
import org.flasck.flas.rewrittenForm.HandlerLambda;
import org.flasck.flas.rewrittenForm.VarNestedFromOuterFunctionScope;
import org.zinutils.collections.CollectionUtils;
import org.zinutils.exceptions.UtilException;
import org.zinutils.graphs.DirectedCyclicGraph;
import org.zinutils.graphs.Link;
import org.zinutils.graphs.Node;
import org.zinutils.graphs.Orchard;
import org.zinutils.graphs.Tree;

public class DependencyAnalyzer {
//	private final ErrorResult errors;

	public DependencyAnalyzer(ErrorResult errors) {
//		this.errors = errors;
	}

	public List<Orchard<RWFunctionDefinition>> analyze(Map<String, RWFunctionDefinition> map) {
		DirectedCyclicGraph<String> dcg = new DirectedCyclicGraph<String>();
		Map<String, RWFunctionDefinition> fdm = new TreeMap<String, RWFunctionDefinition>();
		addFunctionsToDCG(dcg, new TreeMap<String, String>(), fdm, map);
//		System.out.print(dcg);
		return buildOrchards(dcg, fdm);
	}

	private void addFunctionsToDCG(DirectedCyclicGraph<String> dcg, Map<String, String> map, Map<String, RWFunctionDefinition> fdm, Map<String, RWFunctionDefinition> functions) {
		// First make sure all the nodes are in the DCG
		for (Entry<String, RWFunctionDefinition> x : functions.entrySet()) {
			String name = x.getValue().name();
			dcg.ensure(name);

			RWFunctionDefinition fd = x.getValue();
			if (!fd.generate)
				continue;
			fdm.put(name,  fd);
			for (RWFunctionCaseDefn c : fd.cases) {
				for (LocalVar v : c.vars()) {
					String realname = "_var_" + v.uniqueName();
//					System.out.println("Ensuring local var in graph: " + realname);
					dcg.ensure(realname);
					dcg.ensureLink(realname, name);
				}
			}
		}

		// Then add the links
		for (Entry<String, RWFunctionDefinition> x : functions.entrySet()) {
			RWFunctionDefinition fd = x.getValue();
			for (RWFunctionCaseDefn c : fd.cases)
				analyzeExpr(dcg, fd.name(), c.varNames(), c.expr);
		}
	}

	private void analyzeExpr(DirectedCyclicGraph<String> dcg, String name, Set<String> locals, Object expr) {
		if (expr == null)
			return;
//		System.out.println("checking " + name + " against " + expr + " of type " + expr.getClass());
		if (expr instanceof NumericLiteral || expr instanceof StringLiteral || expr instanceof TemplateListVar || expr instanceof FunctionLiteral)
			;
		else if (expr instanceof CardMember) {
			dcg.ensure("_var_" + ((CardMember)expr).uniqueName());
		}
		else if (expr instanceof HandlerLambda) {
			dcg.ensure("_var_" + ((HandlerLambda)expr).uniqueName());
		}
		else if (expr instanceof LocalVar)
			dcg.ensureLink(name, "_var_" + ((LocalVar)expr).uniqueName());
		else if (expr instanceof IterVar)
			// I think because this is synthetic, it's not needed here ...
			; // dcg.ensureLink(name, "_iter_" + ((IterVar)expr).uniqueName());
		else if (expr instanceof PackageVar) {
			dcg.ensure(((PackageVar) expr).id);
			dcg.ensureLink(name, ((PackageVar) expr).id);
		} else if (expr instanceof VarNestedFromOuterFunctionScope) {
			dcg.ensure(((VarNestedFromOuterFunctionScope) expr).id);
			dcg.ensureLink(name, ((VarNestedFromOuterFunctionScope) expr).id);
		} else if (expr instanceof ObjectReference || expr instanceof CardFunction) {
			String orname = ((ExternalRef)expr).uniqueName();
			dcg.ensure(orname);
			dcg.ensureLink(name, orname);
		} else if (expr instanceof ApplyExpr) {
			ApplyExpr ae = (ApplyExpr) expr;
			analyzeExpr(dcg, name, locals, ae.fn);
			for (Object x : ae.args)
				analyzeExpr(dcg, name, locals, x);
		} else if (expr instanceof LetExpr) {
			LetExpr let = (LetExpr) expr;
			analyzeExpr(dcg, name, locals, let.val);
			Set<String> locals2 = new TreeSet<String>(locals);
			locals2.add(let.var);
			analyzeExpr(dcg, name, locals2, let.expr);
		} else
			throw new UtilException("Unhandled expr: " + expr + " of class " + expr.getClass());
	}

	List<Orchard<RWFunctionDefinition>> buildOrchards(DirectedCyclicGraph<String> dcg, Map<String, RWFunctionDefinition> fdm) {
		Set<Set<String>> spanners = new TreeSet<Set<String>>(new SortOnSize());
		for (String name : dcg.nodes()) {
			if (!name.startsWith("_var_"))
				spanners.add(dcg.spanOf(name));
		}
		Set<String> done = new TreeSet<String>();
		List<Set<String>> groups = new ArrayList<Set<String>>();
		for (Set<String> s : spanners) {
			s.removeAll(done);
			if (!s.isEmpty()) {
				groups.add(s);
				done.addAll(s);
			}
		}
		List<Orchard<RWFunctionDefinition>> ret = new ArrayList<Orchard<RWFunctionDefinition>>();
		for (Set<String> g : groups) {
			Orchard<RWFunctionDefinition> orch = buildOrchard(dcg, fdm, g);
			if (!orch.isEmpty())
				ret.add(orch);
		}
		return ret;
	}

	private Orchard<RWFunctionDefinition> buildOrchard(DirectedCyclicGraph<String> dcg, Map<String, RWFunctionDefinition> fdm, Set<String> g) {
//		System.out.println("Attempting to build orchard from " + g);
		Orchard<RWFunctionDefinition> ret = new Orchard<RWFunctionDefinition>();
		Set<String> topCandidates = new TreeSet<String>();

		// Collect together all the people that "defined" variables that were later used
		for (String s : g) {
			if (s.startsWith("_var_"))
				topCandidates.add(CollectionUtils.any(dcg.find(s).linksFrom()).getTo());
		}

//		System.out.println("top candidates = " + topCandidates);
		// Go through this list, seeing which ones don't use other people's variables
		// This must terminate, because scoping, unlike referencing, is tree-based
		for (String s : topCandidates) {
			Node<String> top = dcg.find(s);
			boolean reject = false;
			for (Link<String> l : top.linksFrom()) {
				// If a function depends on a var, check if it is one of its own or an "inherited" one
				// If it's inherited, we reject this candidate.
//				System.out.println(l);
				if (l.getTo().startsWith("_var_")) {
					String s1 = l.getTo().replace("_var_", "");
					s1 = s1.replace(s, "");
					int i1 = s1.indexOf('.');
					int i2 = s1.lastIndexOf('.');
					if (i2 > i1) {
						reject = true;
						System.out.println("Rejecting " + s + " because of " + s1);
					}
				}
			}
			if (reject)
				continue;
			
			// Create a new tree with the definer at the top
			Tree<RWFunctionDefinition> t = ret.addTree(fdm.get(s));
			g.remove(s); // remove everything that we do something with
			for (Link<String> lc : top.linksTo()) {
				// find all the variables it defines and then make their sub-functions our children
				if (lc.getFrom().startsWith("_var")) {
					g.remove(lc.getFrom());
					for (Link<String> lu : lc.getFromNode().linksTo()) {
						g.remove(lu.getFrom());
						if (fdm.containsKey(lu.getFrom())) {
							RWFunctionDefinition to = fdm.get(lu.getFrom());
							if (!to.name().equals(lu.getFrom()))
								t.addChild(t.getRoot(), to);
						}
					}
				}
			}
		}
		
		// when there are no more vars, everything is just a peer
		// add one tree for all remaining items
		for (String s : g)
			if (fdm.containsKey(s))
				ret.addTree(fdm.get(s));

		return ret;
	}
}
