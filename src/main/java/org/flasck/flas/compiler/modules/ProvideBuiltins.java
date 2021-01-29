package org.flasck.flas.compiler.modules;

import org.flasck.flas.errors.ErrorReporter;
import org.flasck.flas.repository.Repository;

public interface ProvideBuiltins {
	public void applyTo(ErrorReporter errors, Repository repository);
}
