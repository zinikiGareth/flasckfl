package org.flasck.flas.vcode.hsieForm;

import org.flasck.flas.blockForm.InputPosition;

public class ScopingClosure extends ClosureCmd {

	public ScopingClosure(InputPosition loc, Var var) {
		super(loc, var);
	}

	@Override
	public boolean justScoping() {
		return true;
	}
}
