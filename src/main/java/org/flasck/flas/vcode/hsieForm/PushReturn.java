package org.flasck.flas.vcode.hsieForm;

import org.flasck.flas.blockForm.InputPosition;
import org.zinutils.reflection.Reflection;

public abstract class PushReturn extends HSIEBlock {
	private String cmd = "PUSH";

	public PushReturn(InputPosition loc) {
		super(loc);
	}

	public PushReturn asReturn() {
		this.cmd = "RETURN";
		return this;
	}
	
	public Object visit(PushVisitor visitor) {
		return Reflection.call(visitor, "visit", this);
	}

	protected abstract Object textValue();

	@Override
	public String toString() {
		return cmd + " " + textValue();
	}
}
