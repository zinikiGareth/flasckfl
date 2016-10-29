package org.flasck.flas.vcode.hsieForm;

import java.util.List;

import org.flasck.flas.blockForm.InputPosition;

public class PushVar extends PushReturn {
	public final CreationOfVar var;
	public final List<CreationOfVar> deps;

	public PushVar(InputPosition loc, CreationOfVar var) {
		super(loc);
		this.var = var;
		this.deps = null;
	}

	public PushVar(InputPosition loc, CreationOfVar var, List<CreationOfVar> deps) {
		super(loc);
		this.var = var;
		this.deps = deps;
		asReturn();
	}

	protected Object textValue() {
		return var + (deps == null? "" : " " + deps);
	}
}
