package org.flasck.flas.testing.golden;

import java.io.PrintWriter;
import java.util.List;
import java.util.Set;

import org.flasck.flas.grammar.Definition;
import org.flasck.flas.grammar.OrProduction;
import org.flasck.flas.grammar.ProductionVisitor;

/** In the grammar, choices are represented as productions a single definition and
 * then other cases (for some reason)
 * This is here to "put that right" for our purposes
 */
public class ChoiceDefinition extends Definition {
	private final List<Definition> choices;

	public ChoiceDefinition(OrProduction prod) {
		choices = prod.allOptions();
	}

	public int quant() {
		return choices.size();
	}
	
	public Definition nth(int n) {
		return choices.get(n);
	}
	@Override
	public void showGrammarFor(PrintWriter str) {
		// TODO Auto-generated method stub

	}

	@Override
	public void collectReferences(Set<String> ret) {
		// TODO Auto-generated method stub

	}

	@Override
	public void collectTokens(Set<String> ret) {
		// TODO Auto-generated method stub

	}

	@Override
	public void visit(ProductionVisitor productionVisitor) {
		// TODO Auto-generated method stub

	}

}
