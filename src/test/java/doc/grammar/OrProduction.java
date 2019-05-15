package doc.grammar;

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class OrProduction extends Production {
	private final List<Definition> defns = new ArrayList<>();
	
	public OrProduction(int ruleNumber, String ruleName, List<Definition> defns) {
		super(ruleNumber, ruleName, defns.get(0));
		this.defns.addAll(defns);
		this.defns.remove(0);
	}

	@Override
	public void show(PrintWriter str) {
		super.show(str);
		for (Definition d : this.defns) {
			str.println("<div class='production-or-block'>");
			str.println("  <div class='production-or'>|</div>");
			d.showGrammarFor(str);
			str.println("</div>");
		}
	}

	@Override
	public void collectReferences(Set<String> ret) {
		super.collectReferences(ret);
		for (Definition d : defns)
			d.collectReferences(ret);
	}

	@Override
	public void collectTokens(Set<String> ret) {
		super.collectTokens(ret);
		for (Definition d : defns)
			d.collectTokens(ret);
	}

	public void visit(ProductionVisitor visitor) {
		List<Definition> ds = new ArrayList<>(defns);
		ds.add(defn);
		visitor.choices(ds);
	}
}
