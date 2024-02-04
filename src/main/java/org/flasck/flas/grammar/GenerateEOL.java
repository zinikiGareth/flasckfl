package org.flasck.flas.grammar;

import java.io.PrintWriter;
import java.util.Set;

public class GenerateEOL extends Definition implements ActionDefinition {

	@Override
	public void showGrammarFor(PrintWriter str) {
		// TODO Auto-generated method stub

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
		productionVisitor.generateEOL();
	}

}
