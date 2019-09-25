package org.flasck.flas.tc3;

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
	public boolean incorporates(Type other) {
		return false;
	}

}
