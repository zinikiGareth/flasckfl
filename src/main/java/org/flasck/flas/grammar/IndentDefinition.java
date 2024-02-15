package org.flasck.flas.grammar;

import java.io.PrintWriter;
import java.util.Set;

public class IndentDefinition extends Definition {
	private final Definition defn;
	private final boolean exactlyOne;
	private final boolean allowZero;
	private String reducesAs;

	public IndentDefinition(Definition defn, boolean exactlyOne, boolean allowZero) {
		this.defn = defn;
		this.exactlyOne = exactlyOne;
		this.allowZero = allowZero;
	}
	
	public Definition indented() {
		return defn;
	}

	@Override
	public void showGrammarFor(PrintWriter str) {
		str.print("<div class='production-nested-block'>");
		str.print("<span class='production-nested'>&gt;&gt;" + (exactlyOne?"!":(allowZero?"":"&gt;")) + "</span>");
		defn.showGrammarFor(str);
		str.print("</div>");
	}

	@Override
	public void collectReferences(Set<String> ret) {
		defn.collectReferences(ret);
	}

	@Override
	public void collectTokens(Set<String> ret) {
		defn.collectTokens(ret);
	}

	@Override
	public void visit(ProductionVisitor productionVisitor) {
		if (!productionVisitor.indent(!allowZero || exactlyOne))
			return;
		if (exactlyOne)
			defn.visit(productionVisitor);
		else if (allowZero)
			productionVisitor.zeroOrMore(defn, true);
		else
			productionVisitor.oneOrMore(defn, true);
		productionVisitor.exdent();
	}
	
	public void reducesAs(String ruleName) {
		this.reducesAs = ruleName;
	}
	
	public boolean canReduceAs(String ruleName) {
		return this.reducesAs != null && this.reducesAs.equals(ruleName);
	}
	
	public String reducesTo() {
		return this.reducesAs;
	}
	
	@Override
	public String toString() {
		return "Indent[" + defn + "]";
	}
}
