package doc.grammar;

import java.io.PrintWriter;
import java.util.Set;

public class ManyDefinition extends Definition {
	private final Definition child;

	public ManyDefinition(Definition child) {
		this.child = child;
	}

	public void showGrammarFor(PrintWriter str) {
		child.showGrammarFor(str);
		str.print("<span class='production-many'>*</span>");
	}

	@Override
	public void collectReferences(Set<String> ret) {
		child.collectReferences(ret);
	}

	@Override
	public void collectTokens(Set<String> ret) {
		child.collectTokens(ret);
	}
}
