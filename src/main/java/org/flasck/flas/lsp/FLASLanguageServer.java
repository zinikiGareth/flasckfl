package org.flasck.flas.lsp;

import java.io.File;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;
import java.util.concurrent.CompletableFuture;

import org.eclipse.lsp4j.InitializeParams;
import org.eclipse.lsp4j.InitializeResult;
import org.eclipse.lsp4j.ServerCapabilities;
import org.eclipse.lsp4j.TextDocumentSyncKind;
import org.eclipse.lsp4j.WorkspaceFolder;
import org.eclipse.lsp4j.services.LanguageServer;
import org.eclipse.lsp4j.services.TextDocumentService;
import org.eclipse.lsp4j.services.WorkspaceService;
import org.flasck.flas.LSPTaskQueue;
import org.flasck.flas.compiler.TaskQueue;
import org.flasck.flas.errors.ErrorReporter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FLASLanguageServer implements LanguageServer {
	private static final Logger logger = LoggerFactory.getLogger("FLASLSP");
	private final ErrorReporter errors;
	private final File flasHome;
	private final TaskQueue taskQ;
    private final FLASFileWatchingService watchingService;
	private final Map<String, Root> roots = new TreeMap<>();
	private String cardsFolder;

	public FLASLanguageServer(ErrorReporter errors, File flasHome) {
		this.errors = errors;
		this.flasHome = flasHome;
		this.taskQ = new LSPTaskQueue();
		this.watchingService = new FLASFileWatchingService(errors, this);
	}

	@Override
	public CompletableFuture<InitializeResult> initialize(InitializeParams params) {
		logger.info("initialize");
		List<WorkspaceFolder> folders = params.getWorkspaceFolders();
		if (folders != null) {
			for (WorkspaceFolder f : folders) {
				addRoot(f.getUri());
			}
		}
        ServerCapabilities capabilities = new ServerCapabilities();
        capabilities.setTextDocumentSync(TextDocumentSyncKind.Full);
        return CompletableFuture.completedFuture(new InitializeResult(capabilities));
	}
	
	public void addRoot(String rootUri) {
		logger.info("opening root " + rootUri);
		try {
			synchronized (roots) {
				URI uri = new URI(rootUri + "/");
				Root root = new Root(errors, taskQ, uri);
				if (roots.containsKey(root.root.getPath()))
					return;
				errors.logMessage("opening root " + root.root);
				roots.put(root.root.getPath(), root);
				root.configure(flasHome);
				root.setCardsFolder(cardsFolder);
				root.gatherFiles();
				root.compileAll(root);
			}
		} catch (URISyntaxException ex) {
			errors.logMessage("could not open " + rootUri);
		}
	}

	public void dispatch(URI uri, String text) {
		Root r = findRoot(uri);
		r.dispatch(uri, text);
	}
	
	private Root findRoot(URI uri) {
		synchronized (roots) {
			String path = uri.getPath();
			for (Entry<String, Root> e : roots.entrySet()) {
				if (path.startsWith(e.getKey()))
					return e.getValue();
			}
			return null;
		}
	}
	
	public void setCardsFolder(String cardsFolder) {
		logger.info("setting cards folder to " + cardsFolder);
		this.cardsFolder = cardsFolder;
		synchronized (roots) {
			for (Root r : roots.values())
				r.setCardsFolder(cardsFolder);
		}
	}

	@Override
	public TextDocumentService getTextDocumentService() {
		return watchingService;
	}

	@Override
	public WorkspaceService getWorkspaceService() {
		return new FLASWorkspaceService(this);
	}

	@Override
	public CompletableFuture<Object> shutdown() {
		logger.info("SHUTDOWN");
        return CompletableFuture.completedFuture(null);
	}

	@Override
	public void exit() {
		logger.info("EXIT");
	}
}
