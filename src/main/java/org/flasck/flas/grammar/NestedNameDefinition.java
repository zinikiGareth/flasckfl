package org.flasck.flas.grammar;

import java.io.PrintWriter;
import java.util.Set;

public class NestedNameDefinition extends Definition implements ActionDefinition {
	private final int offset;

	public NestedNameDefinition(int offset) {
		this.offset = offset;
	}

	@Override
	public void showGrammarFor(PrintWriter str) {
	}

	@Override
	public void collectReferences(Set<String> ret) {
	}

	@Override
	public void collectTokens(Set<String> ret) {
	}

	@Override
	public void visit(ProductionVisitor productionVisitor) {
		productionVisitor.nestName(offset);
	}

}
