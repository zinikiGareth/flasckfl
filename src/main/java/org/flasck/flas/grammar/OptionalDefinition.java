package org.flasck.flas.grammar;

import java.io.PrintWriter;
import java.util.Set;

public class OptionalDefinition extends Definition {
	private final Definition child;
	public final ElseClause elseClause;

	public OptionalDefinition(Definition child, ElseClause elseClause) {
		this.child = child;
		this.elseClause = elseClause;
	}

	@Override
	public void showGrammarFor(PrintWriter str) {
		child.showGrammarFor(str);
		str.print("<span class='production-optional'>?</span>");
	}

	@Override
	public void collectReferences(Set<String> ret) {
		child.collectReferences(ret);
	}

	@Override
	public void collectTokens(Set<String> ret) {
		child.collectTokens(ret);
	}

	@Override
	public void visit(ProductionVisitor productionVisitor) {
		productionVisitor.zeroOrOne(child);
	}

}
