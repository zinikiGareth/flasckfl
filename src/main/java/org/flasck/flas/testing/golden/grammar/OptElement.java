package org.flasck.flas.testing.golden.grammar;

import org.flasck.flas.grammar.Grammar;
import org.flasck.flas.grammar.OptionalDefinition;
import org.flasck.flas.testing.golden.ParsedTokens.GrammarStep;

public class OptElement implements SeqElement {

	public OptElement(Grammar g, OptionalDefinition od) {
		// TODO Auto-generated constructor stub
	}

	@Override
	public MatchResult matchAgainst(GrammarStep mi) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public boolean canBeSkipped() {
		return true;
	}

}
