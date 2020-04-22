package org.flasck.flas.parsedForm.ut;

import org.flasck.flas.commonBase.Expr;
import org.flasck.flas.commonBase.StringLiteral;

public class UnitTestMatch implements UnitTestStep {
	public final Expr card;
	public final MatchedItem what;
	public final StringLiteral selector;
	public final boolean contains;
	public final String text;

	public UnitTestMatch(Expr card, MatchedItem what, StringLiteral selector, boolean contains, String text) {
		this.card = card;
		this.what = what;
		this.selector = selector;
		this.contains = contains;
		this.text = text;
	}
}
