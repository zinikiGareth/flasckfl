package doc.grammar;

import java.io.PrintWriter;
import java.util.Set;

public class Production {
	public final int number;
	public final String name;
	private final Definition defn;

	public Production(int ruleNumber, String ruleName, Definition defn) {
		this.number = ruleNumber;
		this.name = ruleName;
		this.defn = defn;
	}

	public void show(PrintWriter str) {
		str.println("<div class='production-block'>");
		str.println("  <div class='production-number'>(" + number + ")</div>");
		str.println("  <div class='production-name'>" + name + "</div>");
		str.println("  <div class='production-op'>::=</div>");
		defn.showGrammarFor(str);
		str.println("</div>");
	}

	public void collectReferences(Set<String> ret) {
		defn.collectReferences(ret);
	}

	public void collectTokens(Set<String> ret) {
		defn.collectTokens(ret);
	}
}
