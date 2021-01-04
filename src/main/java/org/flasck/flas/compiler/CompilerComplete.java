package org.flasck.flas.compiler;

import org.flasck.flas.Configuration;
import org.flasck.flas.compiler.jsgen.packaging.JSEnvironment;
import org.flasck.flas.errors.ErrorReporter;
import org.zinutils.bytecode.ByteCodeEnvironment;

public interface CompilerComplete {
	void complete(ErrorReporter errors, Configuration config, ByteCodeEnvironment jvm, JSEnvironment jse);
}
