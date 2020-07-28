package org.flasck.flas.grammar;

import java.io.PrintWriter;
import java.util.Set;

public class CondDefinition extends Definition {
	private final String var;
	private final String ne;
	private final boolean notset;
	private final Definition inner;

	public CondDefinition(String var, String ne, boolean notset, Definition inner) {
		this.var = var;
		this.ne = ne;
		this.notset = notset;
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
		if (ne != null)
			productionVisitor.condNotEqual(var, ne, inner);
		else if (notset)
			productionVisitor.condNotSet(var, inner);
		else
			throw new RuntimeException("Cannot handle lack of conditions");
	}

}
