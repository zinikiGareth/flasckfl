package doc.grammar;

import java.io.PrintWriter;
import java.util.Set;

public class IndentDefinition extends Definition {
	private final Definition defn;

	public IndentDefinition(Definition defn) {
		this.defn = defn;
	}

	@Override
	public void showGrammarFor(PrintWriter str) {
		str.print("<div class='production-nested-block'>");
		str.print("<span class='production-nested'>&gt;&gt;</span>");
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
		productionVisitor.indent();
		defn.visit(productionVisitor);
		productionVisitor.exdent();
	}
}
