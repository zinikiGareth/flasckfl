package org.flasck.flas.testing.golden.grammar;

import org.flasck.flas.grammar.Definition;
import org.flasck.flas.grammar.Grammar;
import org.flasck.flas.grammar.ManyDefinition;
import org.flasck.flas.grammar.RefDefinition;
import org.flasck.flas.grammar.TokenDefinition;
import org.flasck.flas.testing.golden.ParsedTokens.GrammarStep;
import org.flasck.flas.testing.golden.ParsedTokens.GrammarToken;
import org.zinutils.exceptions.CantHappenException;
import org.zinutils.exceptions.NotImplementedException;

public class ManyElement implements SeqElement {
	private final Grammar g;
	private final String matchRef;
	private final TokenElement matchTok;

	public ManyElement(Grammar g, ManyDefinition md) {
		this.g = g;
		Definition child = md.repeats();
		if (child instanceof RefDefinition) {
			matchRef = ((RefDefinition)child).ruleName();
			matchTok = null;
		} else if (child instanceof TokenDefinition) {
			matchRef = null;
			matchTok = new TokenElement(g, (TokenDefinition)child);
		} else
			throw new CantHappenException("cannot have a many of " + child.getClass());
	}

	@Override
	public MatchResult matchAgainst(GrammarStep mi) {
		if (mi instanceof GrammarToken) {
			if (matchTok == null) {
				// we were expecting (zero-or-more) trees; if we see a token, the reality "must" be zero ...
				return MatchResult.MANY_NO_MATCH_TRY_NEXT;
			}
			switch (matchTok.matchAgainst(mi)) {
			case SINGLE_MATCH_ADVANCE:
				return MatchResult.MANY_MATCH_MAYBE_MORE;
			case SINGLE_MATCH_FAILED:
				return MatchResult.MANY_NO_MATCH_TRY_NEXT;
			default:
				throw new CantHappenException("token matcher should not return that");
			}
		} else if (mi instanceof GrammarTree) {
			throw new NotImplementedException();
		} else 
			throw new CantHappenException("step is a " + mi.getClass());
	}

	@Override
	public boolean canBeSkipped() {
		return true; // but only if zero-or-more
	}

}
