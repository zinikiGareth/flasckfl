package org.flasck.flas.lsp;

import org.eclipse.lsp4j.services.LanguageClient;
import org.flasck.flas.errors.ErrorReporter;
import org.flasck.flas.repository.Repository;

public interface CompileFile {
	void compile(LanguageClient client, ErrorReporter errors, Repository repository);
}
