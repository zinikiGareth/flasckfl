package org.flasck.flas.compiler;

import java.util.List;

import org.flasck.flas.Configuration;
import org.flasck.flas.compiler.jsgen.packaging.JSEnvironment;
import org.flasck.flas.errors.ErrorReporter;
import org.flasck.jvm.ziniki.PackageSources;
import org.zinutils.bytecode.ByteCodeEnvironment;

public interface CompilerComplete {
	void complete(ErrorReporter errors, Configuration config, List<PackageSources> packages, ByteCodeEnvironment jvm, JSEnvironment jse);
}
