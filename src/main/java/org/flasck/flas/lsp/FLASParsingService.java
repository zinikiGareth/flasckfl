package org.flasck.flas.lsp;

import java.io.File;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Executor;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.eclipse.lsp4j.DidChangeTextDocumentParams;
import org.eclipse.lsp4j.DidCloseTextDocumentParams;
import org.eclipse.lsp4j.DidOpenTextDocumentParams;
import org.eclipse.lsp4j.DidSaveTextDocumentParams;
import org.eclipse.lsp4j.MessageParams;
import org.eclipse.lsp4j.MessageType;
import org.eclipse.lsp4j.services.LanguageClient;
import org.eclipse.lsp4j.services.TextDocumentService;
import org.flasck.flas.compiler.FLASCompiler;
import org.flasck.flas.errors.ErrorReporter;
import org.flasck.flas.repository.Repository;
import org.zinutils.utils.FileNameComparator;

public class FLASParsingService implements TextDocumentService {
	private final BlockingQueue<Runnable> tasks = new LinkedBlockingQueue<Runnable>();
	private final Executor exec = new ThreadPoolExecutor(1, 1, 0L, TimeUnit.SECONDS, tasks);
	private final Map<File, Root> roots = new TreeMap<>(new FileNameComparator());
	private final CompilationSubmitter submitter;
	private LanguageClient client;

	public FLASParsingService(ErrorReporter errors, Repository repository, FLASCompiler compiler) {
		this.submitter = new CompilationSubmitter(errors, repository, tasks, exec);
	}

	public void connect(LanguageClient client) {
		this.client = client;
		this.submitter.connect(client);
	}

	public void addRoot(String rootUri) {
		try {
			URI uri = new URI(rootUri + "/");
			Root root = new Root(client, submitter, uri);
			if (roots.containsKey(root.root))
				return;
			roots.put(root.root, root);
			root.gatherFiles();
		} catch (URISyntaxException ex) {
			client.logMessage(new MessageParams(MessageType.Error, "could not open " + rootUri));
		}
	}

	@Override
	public void didOpen(DidOpenTextDocumentParams params) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void didChange(DidChangeTextDocumentParams params) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void didSave(DidSaveTextDocumentParams params) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void didClose(DidCloseTextDocumentParams params) {
		// TODO Auto-generated method stub
		
	}
}
