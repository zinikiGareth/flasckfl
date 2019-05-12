package doc.grammar;

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class SequenceDefinition extends Definition {
	private List<Definition> elts = new ArrayList<>();
	
	public void add(Definition defn) {
		elts.add(defn);
	}

	@Override
	public void showGrammarFor(PrintWriter str) {
		boolean first = true;
		for (Definition d : elts) {
			if (!first)
				str.print("&nbsp;");
			first = false;
			d.showGrammarFor(str);
		}
	}

	@Override
	public void collectReferences(Set<String> ret) {
		for (Definition d : elts) {
			d.collectReferences(ret);
		}
	}

	@Override
	public void collectTokens(Set<String> ret) {
		for (Definition d : elts) {
			d.collectTokens(ret);
		}
	}

	@Override
	public void visit(ProductionVisitor productionVisitor) {
		for (Definition d : elts)
			d.visit(productionVisitor);
	}
}
