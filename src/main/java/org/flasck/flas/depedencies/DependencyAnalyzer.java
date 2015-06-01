package org.flasck.flas.depedencies;

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
import org.flasck.flas.parsedForm.Scope;
import org.flasck.flas.tokenizers.ExprToken;
import org.zinutils.exceptions.UtilException;
import org.zinutils.graphs.DirectedCyclicGraph;
import org.zinutils.graphs.Orchard;

public class DependencyAnalyzer {
	private final ErrorResult errors;

	public DependencyAnalyzer(ErrorResult errors) {
		this.errors = errors;
	}

	public List<Orchard<FunctionDefinition>> analyze(Scope scope) {
		DirectedCyclicGraph<Object> dcg = new DirectedCyclicGraph<Object>();
		addScopeToDCG(dcg, new TreeMap<String, String>(), scope);
		System.out.print(dcg);
		System.out.print(dcg.roots());
		return null;
	}

	// may need more arguments (like "list of parent scopes" or "map of bound vars to parent scopes")
	private void addScopeToDCG(DirectedCyclicGraph<Object> dcg, Map<String, String> map, Scope scope) {
		for (Entry<String, Entry<String, Object>> x : scope) {
			String name = x.getValue().getKey();
			Object what = x.getValue().getValue();
			
			// TODO: we only want to deal with functions
			// TODO: have we mapped methods to functions yet?
			// TODO: likewise templates if we are going to treat them as FL functions
			if (!(what instanceof FunctionDefinition))
				continue;
			
			dcg.ensure(name);
			FunctionDefinition fd = (FunctionDefinition)what;
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
				addScopeToDCG(dcg, varMap, c.innerScope());
			}
		}
	}

	private void analyzeExpr(DirectedCyclicGraph<Object> dcg, String name, Map<String, String> varMap, Set<String> locals, Object expr) {
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
			case ExprToken.NUMBER: {
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
}
