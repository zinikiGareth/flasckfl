package org.flasck.flas.grammar;

import java.io.PrintWriter;
import java.util.Set;

public class DictSetDefinition extends Definition {
	private final String var;
	private final String val;

	public DictSetDefinition(String var, String val) {
		this.var = var;
		this.val = val;
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
		productionVisitor.setDictEntry(var, val);
	}

}
