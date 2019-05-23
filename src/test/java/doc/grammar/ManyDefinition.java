package doc.grammar;

import java.io.PrintWriter;
import java.util.Set;

public class ManyDefinition extends Definition {
	private final Definition child;
	private final boolean allowZero;

	public ManyDefinition(Definition child, boolean allowZero) {
		this.child = child;
		this.allowZero = allowZero;
	}

	public void showGrammarFor(PrintWriter str) {
		child.showGrammarFor(str);
		str.print("<span class='production-many'>");
		if (allowZero)
			str.print("*");
		else
			str.print("+");
		str.print("</span>");
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
		productionVisitor.zeroOrMore(child);
	}
}
