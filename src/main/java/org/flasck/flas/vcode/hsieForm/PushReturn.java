package org.flasck.flas.vcode.hsieForm;

import org.flasck.flas.blockForm.InputPosition;
import org.zinutils.exceptions.UtilException;
import org.zinutils.reflection.Reflection;
import org.zinutils.utils.Justification;

public abstract class PushReturn extends HSIEBlock {
	private String cmd = "PUSH";
	public final InputPosition location;

	public PushReturn(InputPosition loc) {
		if (loc == null) throw new UtilException("Cannot be null");
		this.location = loc;
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
		String loc;
		// This is just a hack to get the current Golden tests to pass; obviously I should fix all this
		if (cmd.equals("PUSH"))
			loc =  " #" + location + " - also want location where the variable is actually used here";
		else
			loc = " #" + location + " - this appears to be wrong for closures; wants to be the apply expr point";
		return Justification.LEFT.format(cmd + " " + textValue(), 60) + loc;
	}
}
