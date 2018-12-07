package org.flasck.flas.vcode.hsieForm;

import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.flim.BuiltinOperation;

public class PushBuiltin extends PushReturn {
	public final BuiltinOperation bval;

	public PushBuiltin(InputPosition loc, BuiltinOperation bo) {
		super(loc);
		this.bval = bo;
	}
	
	public boolean isField() {
		return this.bval.opName.equals("field");
	}
	
	public boolean isTuple() {
		return this.bval.opName.equals("tuple");
	}

	public boolean isOctor() {
		return this.bval.opName.equals("octor");
	}
	
	public boolean isIdem() {
		return this.bval.opName.equals("idem");
	}

	protected Object textValue() {
		return bval.toString();
	}
	
	@Override
	public String toString() {
		return bval.opName.toUpperCase();
	}
}
