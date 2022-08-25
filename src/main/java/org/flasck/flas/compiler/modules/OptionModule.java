package org.flasck.flas.compiler.modules;

import org.flasck.flas.errors.ErrorReporter;

public interface OptionModule {
	/**
	 * @param errors an ErrorReporter
	 * @param argv the argument vector
	 * @param pos the next argument in the vector to consider
	 * @return the number of arguments used
	 * @throws InvalidUsageException if 
	 */
	int options(ErrorReporter errors, String[] argv, int pos);
}
