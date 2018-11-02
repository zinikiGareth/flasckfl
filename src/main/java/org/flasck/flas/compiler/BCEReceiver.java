package org.flasck.flas.compiler;

import org.zinutils.bytecode.ByteCodeEnvironment;

public interface BCEReceiver {
	void provideBCE(ByteCodeEnvironment bce);
}
