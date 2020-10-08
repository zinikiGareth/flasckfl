package org.flasck.flas.lsp;

import java.io.File;

import org.eclipse.lsp4j.services.LanguageClient;
import org.flasck.flas.errors.ErrorReporter;
import org.flasck.flas.repository.Repository;

public abstract class AbstractCompilation implements CompileFile {
	protected final LanguageClient client;
	protected final File file;
	protected final String inPkg;
	protected final String name;
	protected ErrorReporter errors;
	protected Repository repository;

	public AbstractCompilation(LanguageClient client, File file) {
		this.client = client;
		this.file = file;
		this.name = file.getName();
		File dir = file.getParentFile();
		this.inPkg = dir.getName();
	}

	@Override
	public void bind(ErrorReporter errors, Repository repository) {
		this.errors = errors;
		this.repository = repository;
	}

	@Override
	public int hashCode() {
		return file.hashCode();
	}
	
	@Override
	public boolean equals(Object obj) {
		return obj instanceof AbstractCompilation && file.equals(((AbstractCompilation)obj).file);
	}
}
