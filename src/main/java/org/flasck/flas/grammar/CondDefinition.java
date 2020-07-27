package org.flasck.flas.grammar;

import java.io.PrintWriter;
import java.util.Set;

import org.zinutils.exceptions.NotImplementedException;

public class CondDefinition extends Definition {
	private final String var;
	private final String ne;
	private final Definition inner;

	public CondDefinition(String var, String ne, Definition inner) {
		this.var = var;
		this.ne = ne;
		this.inner = inner;
	}

	@Override
	public void showGrammarFor(PrintWriter str) {
		inner.showGrammarFor(str);
	}

	@Override
	public void collectReferences(Set<String> ret) {
		inner.collectReferences(ret);
	}

	@Override
	public void collectTokens(Set<String> ret) {
		inner.collectTokens(ret);
	}

	@Override
	public void visit(ProductionVisitor productionVisitor) {
		productionVisitor.condNotEqual(var, ne, inner);
	}

}
