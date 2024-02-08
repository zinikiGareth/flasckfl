package org.flasck.flas.grammar;

import java.io.PrintWriter;
import java.util.Set;

public class ManyDefinition extends Definition {
	private final Definition child;
	private final boolean allowZero;
	private final String shared;

	public ManyDefinition(Definition child, boolean allowZero, String shared) {
		this.child = child;
		this.allowZero = allowZero;
		this.shared = shared;
	}
	
	public Definition repeats() {
		return child;
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
		if (shared != null) {
			String cnt = productionVisitor.getTopDictValue(shared);
			if (cnt != null) {
				productionVisitor.exactly(Integer.parseInt(cnt), child, false);
				return;
			}
		}
		int actual;
		if (allowZero)
			actual = productionVisitor.zeroOrMore(child, false);
		else
			actual = productionVisitor.oneOrMore(child, false);
		if (shared != null) {
			productionVisitor.setDictEntry(shared, Integer.toString(actual));
		}
	}
	
	@Override
	public String toString() {
		return "Many[" + child + "]";
	}
}
