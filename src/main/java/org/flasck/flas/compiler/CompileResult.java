package org.flasck.flas.compiler;

import org.zinutils.bytecode.ByteCodeEnvironment;

public class CompileResult {
	public final ByteCodeEnvironment bce;

	public CompileResult(ByteCodeEnvironment bce) {
		this.bce = bce;
	}

}
