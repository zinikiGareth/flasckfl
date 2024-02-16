package org.flasck.flas.testing.golden.grammar;

import org.flasck.flas.grammar.Grammar;
import org.flasck.flas.grammar.TokenDefinition;
import org.flasck.flas.testing.golden.ParsedTokens.GrammarStep;
import org.flasck.flas.testing.golden.ParsedTokens.GrammarToken;
import org.zinutils.exceptions.CantHappenException;

public class TokenElement implements SeqElement {
	private final Grammar g;
	private final TokenDefinition td;

	public TokenElement(Grammar g, TokenDefinition td) {
		this.g = g;
		this.td = td;
	}
	
	@Override
	public MatchResult matchAgainst(GrammarStep mi) {
		if (mi instanceof GrammarToken) {
			GrammarToken tok = (GrammarToken) mi;
			if (td.isToken(g, tok.type, tok.text))
				return MatchResult.SINGLE_MATCH_ADVANCE;
			else
				return MatchResult.SINGLE_MATCH_FAILED;
		} else
			throw new CantHappenException("not a token");
	}
	
	@Override
	public String toString() {
		return "MatchToken[" + td.token() + "]";
	}

	@Override
	public boolean canBeSkipped() {
		return false;
	}

	public boolean canBeKeyword(String keyword) {
		return td.isToken(g, null, keyword);
	}
}
