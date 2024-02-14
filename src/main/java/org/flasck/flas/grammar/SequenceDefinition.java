package org.flasck.flas.grammar;

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.zinutils.exceptions.CantHappenException;

public class SequenceDefinition extends Definition {
	private List<Definition> elts = new ArrayList<>();
	private String baseReducesAs;
	private String optionReducesAs;
	
	public String reducesAs() {
		return baseReducesAs;
	}
	
	public void add(Definition defn) {
		if (defn instanceof ReducesAs) {
			if (baseReducesAs != null)
				throw new CantHappenException("can't define base reduces-as multiple times");
			baseReducesAs = ((ReducesAs)defn).ruleName;
			return;
		}
		if (defn instanceof OptionalDefinition) {
			OptionalDefinition od = (OptionalDefinition) defn;
			if (od.elseClause != null) {
				if (optionReducesAs != null)
					throw new CantHappenException("can't define option reduces-as multiple times");
				optionReducesAs = od.elseClause.ruleName;
			}
		}
		elts.add(defn);
	}

	public boolean canReduceAs(String rule) {
		return rule.equals(baseReducesAs) || rule.equals(optionReducesAs);
	}

	public int length() {
		return elts.size();
	}
	
	public Definition nth(int offset) {
		return elts.get(offset);
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

	public Definition cloneWith(List<Definition> elts) {
		SequenceDefinition ret = new SequenceDefinition();
		ret.elts.addAll(elts);
		ret.baseReducesAs = this.baseReducesAs;
		ret.optionReducesAs = this.optionReducesAs;
		return ret;
	}
}
