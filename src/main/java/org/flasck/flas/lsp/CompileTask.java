package org.flasck.flas.lsp;

import java.io.LineNumberReader;
import java.io.PrintWriter;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.concurrent.BlockingQueue;

import org.eclipse.lsp4j.MessageParams;
import org.eclipse.lsp4j.MessageType;
import org.eclipse.lsp4j.services.LanguageClient;
import org.flasck.flas.repository.Repository;

public class CompileTask implements Runnable {
	private final LanguageClient client;
	private final BlockingQueue<Runnable> tasks;
	private final LSPErrorForwarder errors;
	private final Repository repository;
	private final AbstractCompilation cf;

	public CompileTask(LanguageClient client, BlockingQueue<Runnable> tasks, LSPErrorForwarder errors, Repository repository, CompileFile cf) {
		this.client = client;
		this.tasks = tasks;
		this.errors = errors;
		this.repository = repository;
		this.cf = (AbstractCompilation) cf;
	}

	@Override
	public int hashCode() {
		return cf.file.hashCode();
	}
	
	@Override
	public boolean equals(Object obj) {
		return obj instanceof AbstractCompilation && cf.file.equals(((AbstractCompilation)obj).file);
	}

	@Override
	public void run() {
		System.out.println("Compiling " + cf.file + " with tasks = " + tasks);
		errors.beginProcessing(cf.uri);
		repository.parsing(cf.uri);
		cf.compile(client, errors, repository);
		repository.done();
		errors.doneProcessing();
		if (tasks.isEmpty()) {
			// if there were previously files that were corrupt, try compiling them again
			// do the rest of the compilation
			sendRepo();
		}
	}
	
	private void sendRepo() {
		try {
			StringWriter sw = new StringWriter();
			PrintWriter pw = new PrintWriter(sw);
			repository.dumpTo(pw);
			pw.close();
			LineNumberReader lnr = new LineNumberReader(new StringReader(sw.toString()));
			String s;
			while ((s = lnr.readLine()) != null) {
				client.logMessage(new MessageParams(MessageType.Log, s));
			}
		} catch (Exception ex) {
			client.logMessage(new MessageParams(MessageType.Log, "Error reading repo: " + ex));
		}
	}
	
	@Override
	public String toString() {
		return "CompileTask[" + cf.inPkg + "/" + cf.name + "]";
	}
}
