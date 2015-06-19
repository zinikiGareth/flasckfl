package org.flasck.flas.dependencies;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import org.flasck.flas.errors.ErrorResult;
import org.flasck.flas.parsedForm.AbsoluteVar;
import org.flasck.flas.parsedForm.ApplyExpr;
import org.flasck.flas.parsedForm.CardFunction;
import org.flasck.flas.parsedForm.CardMember;
import org.flasck.flas.parsedForm.ExternalRef;
import org.flasck.flas.parsedForm.FunctionCaseDefn;
import org.flasck.flas.parsedForm.FunctionDefinition;
import org.flasck.flas.parsedForm.HandlerLambda;
import org.flasck.flas.parsedForm.LetExpr;
import org.flasck.flas.parsedForm.LocalVar;
import org.flasck.flas.parsedForm.NumericLiteral;
import org.flasck.flas.parsedForm.ObjectReference;
import org.flasck.flas.parsedForm.StringLiteral;
import org.flasck.flas.parsedForm.TemplateListVar;
import org.zinutils.collections.CollectionUtils;
import org.zinutils.exceptions.UtilException;
import org.zinutils.graphs.DirectedCyclicGraph;
import org.zinutils.graphs.Link;
import org.zinutils.graphs.Node;
import org.zinutils.graphs.Orchard;
import org.zinutils.graphs.Tree;

public class DependencyAnalyzer {
	private final ErrorResult errors;

	public DependencyAnalyzer(ErrorResult errors) {
		this.errors = errors;
	}

	public List<Orchard<FunctionDefinition>> analyze(Map<String, FunctionDefinition> map) {
		DirectedCyclicGraph<String> dcg = new DirectedCyclicGraph<String>();
		Map<String, FunctionDefinition> fdm = new TreeMap<String, FunctionDefinition>();
		addFunctionsToDCG(dcg, new TreeMap<String, String>(), fdm, map);
//		System.out.print(dcg);
		return buildOrchards(dcg, fdm);
	}

	private void addFunctionsToDCG(DirectedCyclicGraph<String> dcg, Map<String, String> map, Map<String, FunctionDefinition> fdm, Map<String, FunctionDefinition> functions) {
		// First make sure all the nodes are in the DCG
		for (Entry<String, FunctionDefinition> x : functions.entrySet()) {
			String name = x.getValue().name;
			dcg.ensure(name);

			FunctionDefinition fd = x.getValue();
			fdm.put(name,  fd);
			int cs = 0;
			for (FunctionCaseDefn c : fd.cases) {
				for (String v : c.intro.allVars()) {
					String realname = "_var_" + name+"_" + cs +"."+v;
//					System.out.println("Ensuring local var in graph: " + realname);
					dcg.ensure(realname);
					dcg.ensureLink(realname, name);
				}
				cs++;
			}
		}

		// Then add the links
		for (Entry<String, FunctionDefinition> x : functions.entrySet()) {
			FunctionDefinition fd = x.getValue();
			for (FunctionCaseDefn c : fd.cases)
				analyzeExpr(dcg, fd.name, c.intro.allVars(), c.expr);
		}
	}

	private void analyzeExpr(DirectedCyclicGraph<String> dcg, String name, Set<String> locals, Object expr) {
		if (expr == null)
			return;
//		System.out.println("checking " + name + " against " + expr + " of type " + expr.getClass());
		if (expr instanceof NumericLiteral || expr instanceof StringLiteral || expr instanceof TemplateListVar)
			;
		else if (expr instanceof CardMember) {
			dcg.ensure("_var_" + ((CardMember)expr).uniqueName());
		}
		else if (expr instanceof HandlerLambda) {
			dcg.ensure("_var_" + ((HandlerLambda)expr).uniqueName());
		}
		else if (expr instanceof LocalVar)
			dcg.ensureLink(name, "_var_" + ((LocalVar)expr).uniqueName());
		else if (expr instanceof AbsoluteVar) {
			dcg.ensure(((AbsoluteVar) expr).id);
			dcg.ensureLink(name, ((AbsoluteVar) expr).id);
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

	List<Orchard<FunctionDefinition>> buildOrchards(DirectedCyclicGraph<String> dcg, Map<String, FunctionDefinition> fdm) {
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
		List<Orchard<FunctionDefinition>> ret = new ArrayList<Orchard<FunctionDefinition>>();
		for (Set<String> g : groups) {
			Orchard<FunctionDefinition> orch = buildOrchard(dcg, fdm, g);
			if (!orch.isEmpty())
				ret.add(orch);
		}
		return ret;
	}

	private Orchard<FunctionDefinition> buildOrchard(DirectedCyclicGraph<String> dcg, Map<String, FunctionDefinition> fdm, Set<String> g) {
//		System.out.println("Attempting to build orchard from " + g);
		Orchard<FunctionDefinition> ret = new Orchard<FunctionDefinition>();
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
			Tree<FunctionDefinition> t = ret.addTree(fdm.get(s));
			g.remove(s); // remove everything that we do something with
			for (Link<String> lc : top.linksTo()) {
				// find all the variables it defines and then make their sub-functions our children
				if (lc.getFrom().startsWith("_var")) {
					g.remove(lc.getFrom());
					for (Link<String> lu : lc.getFromNode().linksTo()) {
						g.remove(lu.getFrom());
						if (fdm.containsKey(lu.getFrom())) {
							FunctionDefinition to = fdm.get(lu.getFrom());
							if (!to.name.equals(lu.getFrom()))
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
