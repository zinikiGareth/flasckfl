package org.flasck.flas.lsp;

import org.flasck.flas.errors.ErrorReporter;
import org.flasck.flas.repository.Repository;

public interface CompileFile extends Runnable {
	void bind(ErrorReporter errors, Repository repository);
}
