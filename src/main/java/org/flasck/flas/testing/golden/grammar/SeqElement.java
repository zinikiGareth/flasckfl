package org.flasck.flas.testing.golden.grammar;

import org.flasck.flas.testing.golden.ParsedTokens.GrammarStep;

public interface SeqElement {

	MatchResult matchAgainst(GrammarStep mi);

	boolean canBeSkipped();

}
