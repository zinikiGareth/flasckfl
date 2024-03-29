package org.flasck.flas.grammar;

import java.io.PrintWriter;
import java.util.Set;


public class CaseNumberingDefinition extends Definition implements ActionDefinition {

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
		productionVisitor.pushCaseNumber();
	}
	
	@Override
	public String toString() {
		return "case-number";
	}
}
