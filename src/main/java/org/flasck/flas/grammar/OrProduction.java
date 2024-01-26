package org.flasck.flas.grammar;

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class OrProduction extends Production {
	private final List<Definition> otherDefns = new ArrayList<>();
	private final List<Definition> choices;
	private final boolean repeatVarName;
	private int maxProb;
	private List<Integer> probs = new ArrayList<>();
	
	public OrProduction(int ruleNumber, String ruleName, List<Definition> defns, boolean repeatVarName) {
		super(ruleNumber, ruleName, defns.get(0));
		this.choices = defns;
		this.repeatVarName = repeatVarName;
		this.otherDefns.addAll(defns);
		this.otherDefns.remove(0);
		this.maxProb = defns.size();
		for (int i=0;i<maxProb;i++)
			this.probs.add(i+1);
	}

	public List<Definition> allOptions() {
		return choices;
	}

	public void add(Definition r) {
		choices.add(r);
		otherDefns.add(r);
		this.probs.add(choices.size());
	}
	
	public int size() {
		return choices.size();
	}
	
	@Override
	public void show(PrintWriter str) {
		super.show(str);
		for (Definition d : this.otherDefns) {
			str.println("<div class='production-or-block'>");
			str.println("  <div class='production-or'>|</div>");
			d.showGrammarFor(str);
			str.println("</div>");
		}
		str.flush();
	}

	@Override
	public void collectReferences(Set<String> ret) {
		super.collectReferences(ret);
		for (Definition d : otherDefns)
			d.collectReferences(ret);
	}

	@Override
	public void collectTokens(Set<String> ret) {
		super.collectTokens(ret);
		for (Definition d : otherDefns)
			d.collectTokens(ret);
	}

	@Override
	public void visit(ProductionVisitor visitor) {
		visitor.choices(this, null, this.choices, this.probs, this.maxProb, this.repeatVarName);
	}

	public void probs(List<Integer> probs) {
		if (size() != probs.size())
			throw new RuntimeException("Have " + probs.size() + " probabilities for " + size() + " cases in " + this.name);
		this.probs = new ArrayList<>();
		this.maxProb = 0;
		for (int i : probs)
			this.probs.add(maxProb += i);
	}

	public void visitWith(Object cxt, ProductionVisitor visitor) {
		visitor.choices(this, cxt, this.choices, this.probs, this.maxProb, this.repeatVarName);
	}

	public boolean wrapUp(Object cxt, ProductionVisitor visitor) {
		return visitor.complete(this, cxt, this.choices);
	}
}
