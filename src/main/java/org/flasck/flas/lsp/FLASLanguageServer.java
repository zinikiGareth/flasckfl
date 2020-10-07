package org.flasck.flas.lsp;

import java.util.concurrent.CompletableFuture;

import org.eclipse.lsp4j.InitializeParams;
import org.eclipse.lsp4j.InitializeResult;
import org.eclipse.lsp4j.ServerCapabilities;
import org.eclipse.lsp4j.services.LanguageClient;
import org.eclipse.lsp4j.services.LanguageClientAware;
import org.eclipse.lsp4j.services.LanguageServer;
import org.eclipse.lsp4j.services.TextDocumentService;
import org.eclipse.lsp4j.services.WorkspaceService;

public class FLASLanguageServer implements LanguageServer, LanguageClientAware {

	@Override
	public void connect(LanguageClient client) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public CompletableFuture<InitializeResult> initialize(InitializeParams params) {
        ServerCapabilities capabilities = new ServerCapabilities();
        return CompletableFuture.completedFuture(new InitializeResult(capabilities));
	}

	@Override
	public TextDocumentService getTextDocumentService() {
		return null;
	}

	@Override
	public WorkspaceService getWorkspaceService() {
		return null;
	}

	@Override
	public CompletableFuture<Object> shutdown() {
        return CompletableFuture.completedFuture(null);
	}

	@Override
	public void exit() {
		// TODO Auto-generated method stub
		
	}

}
