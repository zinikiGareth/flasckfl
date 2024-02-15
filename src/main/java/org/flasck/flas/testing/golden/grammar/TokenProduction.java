package org.flasck.flas.testing.golden.grammar;

import org.flasck.flas.grammar.Grammar;
import org.flasck.flas.grammar.Production;
import org.flasck.flas.grammar.TokenDefinition;

public class TokenProduction implements TrackProduction {
	private Grammar grammar;
	private String name;
	private TokenDefinition d;

	public TokenProduction(Grammar grammar, String name, TokenDefinition d) {
		this.grammar = grammar;
		this.name = d.token();
		this.d = d;
	}

	@Override
	public void initWhenReady(Production prod) {
		// TODO Auto-generated method stub

	}

	@Override
	public TrackProduction choose(String rule) {
		if (name.equals(rule))
			return this;
		else
			return null;
	}
	
	public boolean matches(String text) {
		return d.isToken(grammar, name, text);
	}
	
	@Override
	public String toString() {
		return "token:" + d.token();
	}
}
