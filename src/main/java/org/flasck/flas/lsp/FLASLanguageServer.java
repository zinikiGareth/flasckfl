package org.flasck.flas.lsp;

import java.util.List;
import java.util.concurrent.CompletableFuture;

import org.eclipse.lsp4j.InitializeParams;
import org.eclipse.lsp4j.InitializeResult;
import org.eclipse.lsp4j.ServerCapabilities;
import org.eclipse.lsp4j.TextDocumentSyncKind;
import org.eclipse.lsp4j.WorkspaceFolder;
import org.eclipse.lsp4j.services.LanguageServer;
import org.eclipse.lsp4j.services.TextDocumentService;
import org.eclipse.lsp4j.services.WorkspaceService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FLASLanguageServer implements LanguageServer {
	private static final Logger logger = LoggerFactory.getLogger("FLASLSP");
	private final FLASFileWatchingService watchingService;
	private LSPCore core;

	public FLASLanguageServer(LSPCore core) {
		this.core = core;
		this.watchingService = new FLASFileWatchingService(core.errors(), core);
	}

	@Override
	public CompletableFuture<InitializeResult> initialize(InitializeParams params) {
		logger.info("initialize");
		List<WorkspaceFolder> folders = params.getWorkspaceFolders();
		if (folders != null) {
			for (WorkspaceFolder f : folders) {
				core.addRoot(f.getUri());
			}
		}
        ServerCapabilities capabilities = new ServerCapabilities();
        capabilities.setTextDocumentSync(TextDocumentSyncKind.Full);
        return CompletableFuture.completedFuture(new InitializeResult(capabilities));
	}
	
	@Override
	public TextDocumentService getTextDocumentService() {
		return watchingService;
	}

	@Override
	public WorkspaceService getWorkspaceService() {
		return new FLASWorkspaceService(core);
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
