package org.flasck.flas.tc3;

import org.flasck.flas.blockForm.InputPosition;
import org.zinutils.exceptions.NotImplementedException;

public class ErrorType implements Type {
	@Override
	public String signature() {
		return "ERROR";
	}

	@Override
	public int argCount() {
		return 0;
	}

	@Override
	public Type get(int pos) {
		throw new NotImplementedException();
	}

	@Override
	public boolean incorporates(InputPosition pos, Type other) {
		return false;
	}

	@Override
	public String toString() {
		return "<<ERROR>>";
	}
}
