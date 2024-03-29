package org.flasck.flas.testing.golden.grammar;

import org.flasck.flas.grammar.RefDefinition;
import org.flasck.flas.testing.golden.ParsedTokens.GrammarStep;
import org.flasck.flas.testing.golden.ParsedTokens.GrammarToken;

public class RefElement implements SeqElement {
	private final GrammarChooser chooser;
	private final String rule;

	public RefElement(GrammarChooser chooser, RefDefinition rd) {
		this.chooser = chooser;
		this.rule = rd.ruleName();
	}

	@Override
	public MatchResult matchAgainst(GrammarStep mi) {
		if (mi instanceof GrammarTree) {
			GrammarTree t = (GrammarTree) mi;
			if (t.reducedToRule().equals(rule))
				return MatchResult.MATCH_NESTED;
			else {
				// What is directly descended from here?
				TrackProduction tracker = chooser.rule(rule);
				TrackProduction ret = tracker.choose(t.reducedToRule());
				if (ret == null)
					return MatchResult.SINGLE_MATCH_FAILED;
				else
					return MatchResult.MATCH_NESTED;
			}
		} else if (mi instanceof GrammarToken) {
			GrammarToken tok = (GrammarToken) mi;
			// It obviously can't match the rule itself, but it can match any directly descended TokenElement
			// What is directly descended from here?
			TrackProduction tracker = chooser.rule(rule);
			TrackProduction ret = tracker.choose(tok.type);
			if (ret != null)
				return MatchResult.SINGLE_MATCH_ADVANCE;
			
			// It's also possible that we match by the token text ...
			ret = tracker.choose(tok.text);
			if (ret != null)
				return MatchResult.SINGLE_MATCH_ADVANCE;
			return MatchResult.SINGLE_MATCH_FAILED;
		} else
			return MatchResult.SINGLE_MATCH_FAILED;
	}
	
	public String refersTo() {
		return rule;
	}

	@Override
	public String toString() {
		return "RefElement[" + rule + "]";
	}

	@Override
	public boolean canBeSkipped() {
		return false;
	}

	public TrackProduction choose(String want) {
		if (!chooser.hasRule(want))
			return null;
		TrackProduction ret = chooser.rule(rule);
		if (want.equals(rule))
			return ret;
		else
			return ret.choose(want);
	}
}
