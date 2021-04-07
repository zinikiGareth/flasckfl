package org.flasck.flas.parsedForm.ut;

import org.flasck.flas.parsedForm.TargetZone;
import org.flasck.flas.parsedForm.UnresolvedVar;

public class UnitTestMatch implements UnitTestStep {
	public final UnresolvedVar card;
	public final MatchedItem what;
	public final TargetZone targetZone;
	public final boolean contains;
	public final boolean fails;
	public final String text;

	public UnitTestMatch(UnresolvedVar card, MatchedItem what, TargetZone targetZone, boolean contains, boolean fails, String text) {
		this.card = card;
		this.what = what;
		this.targetZone = targetZone;
		this.contains = contains;
		this.fails = fails;
		this.text = text;
	}
}
