package org.flasck.flas.compiler.jvmgen;

import org.zinutils.bytecode.IExpr;
import org.zinutils.bytecode.MethodDefiner;
import org.zinutils.exceptions.NotImplementedException;

public class JVMCurryArg implements IExpr {
	@Override
	public void spitOutByteCode(MethodDefiner meth) {
		throw new NotImplementedException();
	}

	@Override
	public void flush() {
		throw new NotImplementedException();
	}

	@Override
	public String getType() {
		throw new NotImplementedException();
	}

	@Override
	public int asSource(StringBuilder sb, int ind, boolean b) {
		throw new NotImplementedException();
	}
}