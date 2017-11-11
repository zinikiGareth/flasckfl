package org.flasck.flas.commonBase;

import java.util.List;

import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.vcode.hsieForm.PushBool;
import org.flasck.flas.vcode.hsieForm.PushReturn;
import org.flasck.flas.vcode.hsieForm.Pushable;
import org.flasck.flas.vcode.hsieForm.VarInSource;

public class BooleanLiteral implements Expr, Pushable {
	private final InputPosition location;
	private final boolean value;

	public BooleanLiteral(InputPosition loc, boolean value) {
		this.location = loc;
		this.value = value;
	}

	@Override
	public InputPosition location() {
		return location;
	}

	public boolean value() {
		return value;
	}
	
	@Override
	public PushReturn hsie(InputPosition loc, List<VarInSource> deps) {
		return new PushBool(location, this);
	}

	@Override
	public String toString() {
		return Boolean.toString(value);
	}
}
