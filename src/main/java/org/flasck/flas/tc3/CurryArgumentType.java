package org.flasck.flas.tc3;

import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.commonBase.Locatable;
import org.zinutils.exceptions.NotImplementedException;

public class CurryArgumentType implements Type, Locatable {
	private final InputPosition loc;

	public CurryArgumentType(InputPosition loc) {
		this.loc = loc;
	}

	@Override
	public InputPosition location() {
		return loc;
	}

	@Override
	public String signature() {
		throw new NotImplementedException();
	}

	@Override
	public int argCount() {
		throw new NotImplementedException();
	}

	@Override
	public Type get(int pos) {
		throw new NotImplementedException();
	}

	@Override
	public boolean incorporates(Type other) {
		throw new NotImplementedException();
	}
}
