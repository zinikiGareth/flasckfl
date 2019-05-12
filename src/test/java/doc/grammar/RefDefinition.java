package doc.grammar;

import java.io.PrintWriter;
import java.util.Set;

import org.apache.commons.lang3.StringEscapeUtils;

public class RefDefinition extends Definition {
	private final String child;

	public RefDefinition(String child) {
		this.child = child;
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

	@Override
	public void visit(ProductionVisitor productionVisitor) {
		productionVisitor.referTo(child);
	}

}
