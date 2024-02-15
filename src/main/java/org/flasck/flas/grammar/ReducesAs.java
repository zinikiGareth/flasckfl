package org.flasck.flas.grammar;

import java.io.PrintWriter;
import java.util.Set;

import org.zinutils.exceptions.NotImplementedException;

public class ReducesAs extends Definition {
	public final String ruleName;
	public final String base;

	public ReducesAs(String reducesAs, String base) {
		ruleName = reducesAs;
		this.base = base;
	}

	@Override
	public void showGrammarFor(PrintWriter str) {
		throw new NotImplementedException();
	}

	@Override
	public void collectReferences(Set<String> ret) {
		throw new NotImplementedException();
	}

	@Override
	public void collectTokens(Set<String> ret) {
		throw new NotImplementedException();
	}

	@Override
	public void visit(ProductionVisitor productionVisitor) {
		throw new NotImplementedException();
	}
}
