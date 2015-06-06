package org.flasck.flas.depedencies;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import org.flasck.flas.ErrorResult;
import org.flasck.flas.parsedForm.ApplyExpr;
import org.flasck.flas.parsedForm.FunctionCaseDefn;
import org.flasck.flas.parsedForm.FunctionDefinition;
import org.flasck.flas.parsedForm.ItemExpr;
import org.flasck.flas.tokenizers.ExprToken;
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
		for (Entry<String, FunctionDefinition> x : functions.entrySet()) {
			String name = x.getValue().name;
			dcg.ensure(name);

			FunctionDefinition fd = x.getValue();
			fdm.put(name,  fd);
			for (FunctionCaseDefn c : fd.cases) {
				Set<String> locals = new TreeSet<String>();
				c.intro.gatherVars(locals);
				Map<String, String> varMap = new TreeMap<String, String>(map);
				for (String v : locals) {
					String realname = "_var_" + name+"__"+v;
					dcg.ensure(realname);
					dcg.ensureLink(realname, name);
					varMap.put("_scoped."+v, realname);
				}
				analyzeExpr(dcg, name, varMap, locals, c.expr);
			}
		}
	}

	private void analyzeExpr(DirectedCyclicGraph<String> dcg, String name, Map<String, String> varMap, Set<String> locals, Object expr) {
		if (expr == null)
			return;
		if (expr instanceof ItemExpr) {
			ExprToken tok = ((ItemExpr)expr).tok;
			switch(tok.type) {
			case ExprToken.IDENTIFIER: {
				if (locals.contains(tok.text)) {
					// do nothing
				} else if (varMap.containsKey(tok.text)) {
					// link to the fully scoped name and thus to the guy that introduced it
					dcg.ensureLink(name, varMap.get(tok.text));
				} else {
					// a global name
					dcg.ensure(tok.text);
					dcg.ensureLink(name, tok.text);
				}
				break;
			}
			case ExprToken.NUMBER:
			case ExprToken.STRING:
			{
				// that's OK
				break;
			}
			default: throw new UtilException("Can't handle " + tok);
			}
		} else if (expr instanceof ApplyExpr) {
			ApplyExpr ae = (ApplyExpr) expr;
			analyzeExpr(dcg, name, varMap, locals, ae.fn);
			for (Object x : ae.args)
				analyzeExpr(dcg, name, varMap, locals, x);
		} else
			throw new UtilException("Unhandled expr: " + expr + " -> " + expr.getClass());
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
		Orchard<FunctionDefinition> ret = new Orchard<FunctionDefinition>();
		Set<String> topCandidates = new TreeSet<String>();

		// Collect together all the people that "defined" variables that were later used
		for (String s : g) {
			if (s.startsWith("_var_"))
				topCandidates.add(CollectionUtils.any(dcg.find(s).linksFrom()).getTo());
		}

		// Go through this list, seeing which ones don't use other people's variables
		// This must terminate, because scoping, unlike referencing, is tree-based
		for (String s : topCandidates) {
			Node<String> top = dcg.find(s);
			for (Link<String> l : top.linksFrom()) {
				// TODO: handle the case where a function both defines and uses a var
				if (l.getTo().startsWith("_var_"))
					throw new UtilException("The non-top-candidate case");
			}
			
			// Create a new tree with the definer at the top
			Tree<FunctionDefinition> t = ret.addTree(fdm.get(s));
			g.remove(s); // remove everything that we do something with
			for (Link<String> lc : top.linksTo()) {
				// find all the variables it defines and then make their sub-functions our children
				if (lc.getFrom().startsWith("_var")) {
					g.remove(lc.getFrom());
					for (Link<String> lu : lc.getFromNode().linksTo()) {
						g.remove(lu.getFrom());
						if (fdm.containsKey(lu.getFrom()))
							t.addChild(t.getRoot(), fdm.get(lu.getFrom()));
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
