package org.flasck.flas.vcode.hsieForm;

import org.flasck.flas.blockForm.InputPosition;

// One of the things that HSIE does is to roll multiple "different" things
// into being the "same" thing.  For instance:
//   take k Nil = ...
//   take n (a:l) = ...
// Both 'k' and 'n' are defined to be var0.  But they came from different places and have different names.
// This class is supposed to "undo" that while keeping the benefits.  We'll see ...

public class CreationOfVar {
	public final Var var;
	public final InputPosition loc;
	public final String called;

	public CreationOfVar(Var var, InputPosition loc, String called) {
		this.var = var;
		this.loc = loc;
		this.called = called;
	}

	@Override
	public int hashCode() {
		return var.hashCode();
	}

	@Override
	public boolean equals(Object obj) {
		if (obj instanceof Var)
			return this.var.equals(obj);
		else if (obj instanceof CreationOfVar)
			return var.equals(((CreationOfVar)obj).var);
		else
			return false;
	}


	@Override
	public String toString() {
		return var + ":" + called;
	}
}