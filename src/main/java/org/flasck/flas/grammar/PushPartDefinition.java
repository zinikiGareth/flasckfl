package org.flasck.flas.grammar;

import java.io.PrintWriter;
import java.util.Set;

public class PushPartDefinition extends Definition {
	private final String prefix;
	private final String names;
	private final boolean appendFileName;

	public PushPartDefinition(String prefix, String names, boolean appendFileName) {
		this.prefix = prefix;
		this.names = names;
		this.appendFileName = appendFileName;
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
		productionVisitor.pushPart(prefix, names, appendFileName);
	}

}
