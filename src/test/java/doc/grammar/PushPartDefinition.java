package doc.grammar;

import java.io.PrintWriter;
import java.util.Set;

public class PushPartDefinition extends Definition {
	private final String prefix;

	public PushPartDefinition(String prefix) {
		this.prefix = prefix;
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
		productionVisitor.pushPart(prefix);
	}

}
