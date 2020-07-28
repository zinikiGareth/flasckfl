package org.flasck.flas.grammar;

import java.io.PrintWriter;
import java.util.Set;

public class DictClearDefinition extends Definition {
	private final String var;

	public DictClearDefinition(String var) {
		this.var = var;
	}

	@Override
	public void showGrammarFor(PrintWriter str) {
	}

	@Override
	public void collectReferences(Set<String> ret) {
		// TODO Auto-generated method stub

	}

	@Override
	public void collectTokens(Set<String> ret) {
		// TODO Auto-generated method stub

	}

	@Override
	public void visit(ProductionVisitor productionVisitor) {
		productionVisitor.clearDictEntry(var);
	}

}
