package org.flasck.flas.vcode.hsieForm;

import java.util.List;

import org.flasck.flas.blockForm.InputPosition;

public class PushVar extends PushReturn {
	public final VarInSource var;
	public final List<VarInSource> deps;

	public PushVar(InputPosition loc, VarInSource var, List<VarInSource> deps) {
		super(loc);
		this.var = var;
		this.deps = deps;
	}

	protected Object textValue() {
		return var + (deps == null? "" : " " + deps);
	}
}
