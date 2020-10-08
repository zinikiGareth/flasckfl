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
	private final ErrorReporter errors;
	private final Repository repository;
	private final FLASCompiler compiler;
	private final BlockingQueue<CompileFile> tasks = new LinkedBlockingQueue<CompileFile>();
	@SuppressWarnings({ "rawtypes", "unchecked" })
	private final Executor exec = new ThreadPoolExecutor(1, 1, 0L, TimeUnit.SECONDS, (BlockingQueue<Runnable>)(BlockingQueue)tasks);
	private final Map<File, Root> roots = new TreeMap<>(new FileNameComparator());
	private LanguageClient client;

	public FLASParsingService(ErrorReporter errors, Repository repository, FLASCompiler compiler) {
		this.errors = errors;
		this.repository = repository;
		this.compiler = compiler;
	}

	public void connect(LanguageClient client) {
		this.client = client;
	}

	public void addRoot(String rootUri) {
		try {
			URI uri = new URI(rootUri + "/");
			File root = new File(uri.getPath());
			if (roots.containsKey(root))
				return;
			client.logMessage(new MessageParams(MessageType.Log, "opening root " + root));
			Root rootedAt = new Root(client, new CompilationSubmitter(errors, repository, tasks, exec), root);
			roots.put(root, rootedAt);
			rootedAt.gatherFiles();
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
