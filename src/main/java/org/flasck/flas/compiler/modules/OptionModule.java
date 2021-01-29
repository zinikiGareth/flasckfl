package org.flasck.flas.compiler.modules;

import org.flasck.flas.errors.ErrorReporter;

public interface OptionModule {
	int options(ErrorReporter errors, String[] argv, int pos);
}
