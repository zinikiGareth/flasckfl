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
	private final TrackProduction matchRef;
	private final TokenElement matchTok;

	public ManyElement(GrammarChooser chooser, Grammar g, ManyDefinition md) {
		Definition child = md.repeats();
		if (child instanceof RefDefinition) {
			String rule = ((RefDefinition)child).ruleName();
			matchRef = chooser.rule(rule);
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
			GrammarToken tok = (GrammarToken) mi;
			if (matchTok != null) {
				return encode(matchTok.matchAgainst(tok));
			} else if (matchRef != null) {
				if (matchRef.choose(tok.type) != null)
					return MatchResult.MANY_MATCH_MAYBE_MORE;
				else
					return MatchResult.MANY_NO_MATCH_TRY_NEXT;
			} else
				throw new CantHappenException("what are we matching?");
		} else if (mi instanceof GrammarTree) {
			return MatchResult.MATCH_NESTED;
		} else 
			throw new CantHappenException("step is a " + mi.getClass());
	}

	private MatchResult encode(MatchResult matched) {
		switch (matched) {
		case SINGLE_MATCH_ADVANCE:
			return MatchResult.MANY_MATCH_MAYBE_MORE;
		case SINGLE_MATCH_FAILED:
			return MatchResult.MANY_NO_MATCH_TRY_NEXT;
		default:
			throw new CantHappenException("token matcher should not return that");
		}
	}
	
	public boolean matchesRef() {
		return matchRef != null;
	}
	
	public TrackProduction matchRef() {
		return matchRef;
	}

	@Override
	public boolean canBeSkipped() {
		return true; // but only if zero-or-more
	}

	@Override
	public String toString() {
		return "ManyElt[" + matchRef+":" + matchTok + "]";
	}
}
