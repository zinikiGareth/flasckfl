package org.flasck.flas.grammar;

import java.io.PrintWriter;
import java.util.Set;

import org.apache.commons.lang3.StringEscapeUtils;

public class RefDefinition extends Definition {
	private final String child;
	private final boolean resetToken;
	private final int from;
	private final int to;

	public RefDefinition(String child, boolean resetToken, int from, int to) {
		this.child = child;
		this.resetToken = resetToken;
		this.from = from;
		this.to = to;
	}

	public boolean refersTo(String rule) {
		return child.equals(rule);
	}
	
	public Production production(Grammar grammar) {
		return grammar.findRule(this.child);
	}

	@Override
	public void showGrammarFor(PrintWriter str) {
		str.append("<span class='production-reference'>&lt;" + StringEscapeUtils.escapeHtml4(child) + "&gt;</span>");
	}

	@Override
	public void collectReferences(Set<String> ret) {
		ret.add(child);
	}

	@Override
	public void collectTokens(Set<String> ret) {
	}
	
	public OrProduction isOr(ProductionVisitor productionVisitor) {
		return productionVisitor.isOr(child);
	}

	@Override
	public void visit(ProductionVisitor productionVisitor) {
		productionVisitor.referTo(child, resetToken);
	}
	
	public int getFrom() {
		return from;
	}
	
	public int getTo() {
		return to;
	}

	@Override
	public String toString() {
		return "Ref[" + child + "]";
	}
}
