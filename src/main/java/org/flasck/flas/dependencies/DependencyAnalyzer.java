package org.flasck.flas.dependencies;

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeSet;

import org.flasck.flas.commonBase.ApplyExpr;
import org.flasck.flas.commonBase.IfExpr;
import org.flasck.flas.commonBase.NumericLiteral;
import org.flasck.flas.commonBase.StringLiteral;
import org.flasck.flas.commonBase.template.TemplateListVar;
import org.flasck.flas.rewrittenForm.AssertTypeExpr;
import org.flasck.flas.rewrittenForm.CardFunction;
import org.flasck.flas.rewrittenForm.CardMember;
import org.flasck.flas.rewrittenForm.CardStateRef;
import org.flasck.flas.rewrittenForm.DeferredSendExpr;
import org.flasck.flas.rewrittenForm.ExternalRef;
import org.flasck.flas.rewrittenForm.FunctionLiteral;
import org.flasck.flas.rewrittenForm.HandlerLambda;
import org.flasck.flas.rewrittenForm.IterVar;
import org.flasck.flas.rewrittenForm.LocalVar;
import org.flasck.flas.rewrittenForm.ObjectReference;
import org.flasck.flas.rewrittenForm.PackageVar;
import org.flasck.flas.rewrittenForm.RWFunctionCaseDefn;
import org.flasck.flas.rewrittenForm.RWFunctionDefinition;
import org.flasck.flas.rewrittenForm.TypeCheckMessages;
import org.flasck.flas.rewrittenForm.VarNestedFromOuterFunctionScope;
import org.zinutils.exceptions.UtilException;
import org.zinutils.graphs.DirectedCyclicGraph;

public class DependencyAnalyzer {
	final DirectedCyclicGraph<String> dcg = new DirectedCyclicGraph<String>();

	public DependencyAnalyzer() {
	}

	public List<Set<RWFunctionDefinition>> analyze(Map<String, RWFunctionDefinition> functions) {
		for (Entry<String, RWFunctionDefinition> x : functions.entrySet())
			addFunctionToDCG(x.getKey(), x.getValue());
		for (Entry<String, RWFunctionDefinition> x : functions.entrySet())
			addLinksToDCG(x.getKey(), x.getValue());
		return group(functions);
	}

	private void addFunctionToDCG(String name, RWFunctionDefinition fd) {
		dcg.ensure(name);

		if (!fd.generate)
			return;
		for (RWFunctionCaseDefn c : fd.cases) {
			for (LocalVar v : c.vars()) {
				String varUniqueName = v.uniqueName();
				dcg.ensure(varUniqueName);
				
				// There is a circular dependency between functions and the vars they introduce
				dcg.ensureLink(varUniqueName, name);
				dcg.ensureLink(name, varUniqueName);
			}
		}
	}

	private void addLinksToDCG(String name, RWFunctionDefinition fd) {
		for (RWFunctionCaseDefn c : fd.cases)
			try {
				analyzeExpr(name, c.varNames(), c.expr);
			} catch (UtilException ex) {
				ex.printStackTrace();
				System.out.println(dcg);
				throw ex;
			}
	}

	@SuppressWarnings("unchecked")
	private void analyzeExpr(String name, Set<String> locals, Object expr) {
		if (expr == null)
			return;
//		System.out.println("checking " + name + " against " + expr + " of type " + expr.getClass());
		if (expr instanceof NumericLiteral || expr instanceof StringLiteral || expr instanceof TemplateListVar || expr instanceof FunctionLiteral)
			;
		else if (expr instanceof CardStateRef)
			; // I don't think this introduces dependencies between functions, just on the card
		else if (expr instanceof CardMember) {
			dcg.ensure(((CardMember)expr).uniqueName());
		}
		else if (expr instanceof HandlerLambda) {
			HandlerLambda hl = (HandlerLambda) expr;
			dcg.ensure(hl.uniqueName());
			if (hl.scopedFrom != null)
				analyzeExpr(name, locals, hl.scopedFrom);
		}
		else if (expr instanceof LocalVar) {
			String un = ((LocalVar)expr).uniqueName();
			dcg.ensureLink(name, un);
		} else if (expr instanceof IterVar)
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
			analyzeExpr(name, locals, ae.fn);
			analyzeExpr(name, locals, ae.args);
		} else if (expr instanceof List) {
			for (Object x : (List<Object>)expr)
				analyzeExpr(name, locals, x);
		} else if (expr instanceof IfExpr) {
			IfExpr ie = (IfExpr) expr;
			analyzeExpr(name, locals, ie.guard);
			analyzeExpr(name, locals, ie.ifExpr);
			analyzeExpr(name, locals, ie.elseExpr);
		} else if (expr instanceof TypeCheckMessages) {
			TypeCheckMessages tcm = (TypeCheckMessages) expr;
			analyzeExpr(name, locals, tcm.expr);
		} else if (expr instanceof AssertTypeExpr) {
			AssertTypeExpr tcm = (AssertTypeExpr) expr;
			analyzeExpr(name, locals, tcm.expr);
		} else if (expr instanceof DeferredSendExpr) {
			DeferredSendExpr dse = (DeferredSendExpr) expr;
			analyzeExpr(name, locals, dse.sender);
			analyzeExpr(name, locals, dse.args);
		} else
			throw new UtilException("Unhandled expr: " + expr + " of class " + expr.getClass());
	}

	List<Set<RWFunctionDefinition>> group(Map<String, RWFunctionDefinition> functions) {
		// First build a "list" of all the function groups, from least complicated to most complicated
		TreeSet<Set<RWFunctionDefinition>> order = new TreeSet<Set<RWFunctionDefinition>>(new SortOnSize());
		for (RWFunctionDefinition s : functions.values()) {
			if (!s.generate)
				continue;
			Set<RWFunctionDefinition> mine = functionsIn(functions, dcg.spanOf(s.name()));
			order.add(mine);
		}
		
		// Now convert it to a list
		return new ArrayList<Set<RWFunctionDefinition>>(order);
	}

	private Set<RWFunctionDefinition> functionsIn(Map<String, RWFunctionDefinition> functions, Set<String> spanOf) {
		Set<RWFunctionDefinition> ret = new HashSet<RWFunctionDefinition>();
		for (String s : spanOf)
			if (functions.containsKey(s))
				ret.add(functions.get(s));
		return ret;
	}

	public void dump(PrintWriter pw) {
		pw.print(dcg);
		pw.println("========");
	}
}
