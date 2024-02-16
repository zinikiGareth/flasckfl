package org.flasck.flas.testing.golden.grammar;

public enum MatchResult {
	SINGLE_MATCH_ADVANCE,
	SINGLE_MATCH_FAILED,
	MANY_MATCH_MAYBE_MORE,
	MANY_NO_MATCH_TRY_NEXT,
	MATCH_NESTED,
	MATCH_NESTED_MAYBE_MORE
}
