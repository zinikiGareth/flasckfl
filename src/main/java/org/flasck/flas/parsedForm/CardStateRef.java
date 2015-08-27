package org.flasck.flas.parsedForm;

import org.flasck.flas.blockForm.InputPosition;

// This should possibly be called "ThisReference"
// It is intended to be a holder for the "instanceof" of a containing Card, either from Card methods OR from nested Card Handlers, etc. 
public class CardStateRef {
	public final InputPosition location;
	public final boolean fromHandler;

	public CardStateRef(InputPosition location, boolean fromHandler) {
		this.location = location;
		this.fromHandler = fromHandler;
	}

	public InputPosition location() {
		return location;
	}

	public boolean fromHandler() {
		return fromHandler;
	}
	
	@Override
	public String toString() {
		return fromHandler? "this._card" : "this";
	}
}
