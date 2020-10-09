package org.flasck.flas.lsp;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Executor;

import org.eclipse.lsp4j.services.LanguageClient;
import org.flasck.flas.repository.Repository;

public class CompilationSubmitter implements Submitter {
	private final LSPErrorForwarder errors;
	private final ParseURI parser;
	private final Repository repository;
	private final BlockingQueue<Runnable> tasks;
	private final Executor exec;
	private LanguageClient client;
	private String cardsFolder;

	public CompilationSubmitter(ParseURI parser, LSPErrorForwarder errors, Repository repository, BlockingQueue<Runnable> tasks, Executor exec) {
		this.errors = errors;
		this.parser = parser;
		this.repository = repository;
		this.tasks = tasks;
		this.exec = exec;
	}

	public void connect(LanguageClient client) {
		this.client = client;
	}

	public void setCardsFolder(String cardsFolder) {
		this.cardsFolder = cardsFolder;
	}

	@SuppressWarnings("unlikely-arg-type") // we store compile tasks but have equals rigged up to compare to CompileFile
	@Override
	public void submit(CompileFile cf) {
		if (!tasks.contains(cf)) {// don't add things multiple times
			if (cf != null) {
				exec.execute(new CompileTask(client, parser, tasks, errors, repository, cardsFolder, cf));
			}
		}
	}
}
