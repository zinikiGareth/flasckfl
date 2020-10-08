package org.flasck.flas.lsp;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Executor;

import org.flasck.flas.errors.ErrorReporter;
import org.flasck.flas.repository.Repository;

public class CompilationSubmitter implements Submitter {
	private final ErrorReporter errors;
	private final Repository repository;
	private final BlockingQueue<CompileFile> tasks;
	private final Executor exec;

	public CompilationSubmitter(ErrorReporter errors, Repository repository, BlockingQueue<CompileFile> tasks, Executor exec) {
		this.errors = errors;
		this.repository = repository;
		this.tasks = tasks;
		this.exec = exec;
	}

	@Override
	public void submit(CompileFile cf) {
		if (!tasks.contains(cf)) {// don't add things multiple times
			if (cf != null) {
				cf.bind(errors, repository);
				exec.execute(cf);
			}
		}
	}

}
