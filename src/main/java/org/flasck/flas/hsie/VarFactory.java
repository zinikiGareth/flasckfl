package org.flasck.flas.hsie;

import org.flasck.flas.vcode.hsieForm.Var;

public class VarFactory {
	int nextVar = 0;

	public Var nextVar() {
		return new Var(nextVar++);
	}

}
