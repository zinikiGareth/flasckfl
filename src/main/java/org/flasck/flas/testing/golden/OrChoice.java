package org.flasck.flas.testing.golden;

import org.flasck.flas.grammar.Definition;
import org.flasck.flas.grammar.Grammar;
import org.flasck.flas.grammar.OrProduction;
import org.flasck.flas.grammar.Production;
import org.flasck.flas.grammar.RefDefinition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class OrChoice implements GrammarLevel {
	public static final Logger logger = LoggerFactory.getLogger("GrammarChecker");
	private final Grammar grammar;
	private final OrProduction prod;

	public OrChoice(Grammar grammar, OrProduction rule) {
		this.grammar = grammar;
		this.prod = rule;
	}
	
	@Override
	public boolean is(String rule) {
		return this.prod.equals(rule);
	}

	
	@Override
	public Production choose(String rule) {
		for (Definition d : prod.allOptions()) {
			logger.info("considering " + d);
			if (d instanceof RefDefinition && ((RefDefinition)d).refersTo(rule))
				return grammar.findRule(rule);
		}
		return null;
	}

	@Override
	public String toString() {
		return prod.name;
	}
}
