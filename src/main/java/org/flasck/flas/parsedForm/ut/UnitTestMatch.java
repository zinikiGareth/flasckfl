package org.flasck.flas.parsedForm.ut;

public class UnitTestMatch implements UnitTestStep {
	public final MatchedItem what;
	public final String selector;
	public final boolean contains;
	public final String text;

	public UnitTestMatch(MatchedItem what, String selector, boolean contains, String text) {
		this.what = what;
		this.selector = selector;
		this.contains = contains;
		this.text = text;
	}
}
