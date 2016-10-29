package org.flasck.flas.vcode.hsieForm;

import org.flasck.flas.blockForm.InputPosition;
import org.zinutils.reflection.Reflection;

public abstract class PushReturn extends HSIEBlock {
	private String cmd = "PUSH";

	public PushReturn(InputPosition loc) {
		super(loc);
	}

	public void asReturn() {
		this.cmd = "RETURN";
	}
	
	public Object visit(PushVisitor visitor) {
		return Reflection.call(visitor, "visit", this);
	}

	protected abstract Object textValue();

	@Override
	public String toString() {
		return cmd + " " + textValue();
	}

	//TODO: HSIE: remove this, which should be dead
	public boolean isPush() {
		return cmd.equals("PUSH");
	}
}
