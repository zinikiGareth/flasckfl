package doc.grammar;

import java.io.PrintWriter;
import java.util.Set;

public class WillNameDefinition extends Definition {
	private final String amended;
	private final String pattern;

	public WillNameDefinition(String amended, String pattern) {
		this.amended = amended;
		this.pattern = pattern;
	}

	@Override
	public void showGrammarFor(PrintWriter str) {
		throw new org.zinutils.exceptions.NotImplementedException();
	}

	@Override
	public void collectReferences(Set<String> ret) {
		throw new org.zinutils.exceptions.NotImplementedException();
	}

	@Override
	public void collectTokens(Set<String> ret) {
		throw new org.zinutils.exceptions.NotImplementedException();
	}

	@Override
	public void visit(ProductionVisitor productionVisitor) {
		productionVisitor.futurePattern(amended, pattern);
	}

}
