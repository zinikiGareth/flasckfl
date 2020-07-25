package doc.grammar;

import java.io.PrintWriter;
import java.util.Set;

public class Production {
	public final int number;
	public final String name;
	protected final Definition defn;
	private boolean needsMoreTesting;

	public Production(int ruleNumber, String ruleName, Definition defn) {
		this.number = ruleNumber;
		this.name = ruleName;
		this.defn = defn;
	}

	public String ruleName() {
		return name;
	}

	public int ruleNumber() {
		return number;
	}
	
	public void show(PrintWriter str) {
		str.println("<div class='production-block'>");
		str.println("  <div class='production-number" + highlightTestingNeeded() + "'>(" + number + ")</div>");
		str.println("  <div class='production-name'>" + name + "</div>");
		str.println("  <div class='production-op'>::=</div>");
		defn.showGrammarFor(str);
		str.println("</div>");
	}

	private String highlightTestingNeeded() {
		return needsMoreTesting?" production-needs-testing":"";
	}

	public void collectReferences(Set<String> ret) {
		defn.collectReferences(ret);
	}

	public void collectTokens(Set<String> ret) {
		defn.collectTokens(ret);
	}

	public void visit(ProductionVisitor visitor) {
		visitor.visit(defn);
	}

	public void needsMoreTesting() {
		this.needsMoreTesting = true;
	}
}
