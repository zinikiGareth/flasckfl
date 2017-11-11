package org.flasck.flas.rewrittenForm;

import java.util.List;

import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.vcode.hsieForm.PushCSR;
import org.flasck.flas.vcode.hsieForm.PushReturn;
import org.flasck.flas.vcode.hsieForm.Pushable;
import org.flasck.flas.vcode.hsieForm.VarInSource;

// This should possibly be called "ThisReference"
// It is intended to be a holder for the "instanceof" of a containing Card, either from Card methods OR from nested Card Handlers, etc. 
public class CardStateRef implements Pushable{
	public final InputPosition location;
	public final boolean fromHandler;

	public CardStateRef(InputPosition location, boolean fromHandler) {
		this.location = location;
		this.fromHandler = fromHandler;
	}

	public InputPosition location() {
		return location;
	}

	@Override
	public PushReturn hsie(InputPosition loc, List<VarInSource> deps) {
		return new PushCSR(location, this);
	}

	public boolean fromHandler() {
		return fromHandler;
	}
	
	@Override
	public String toString() {
		return fromHandler? "this._card" : "this";
	}
}
