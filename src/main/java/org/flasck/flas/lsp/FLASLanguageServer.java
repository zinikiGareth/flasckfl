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
import org.flasck.flas.compiler.FLASCompiler;
import org.flasck.flas.repository.Repository;

public class FLASLanguageServer implements LanguageServer, LanguageClientAware {
    private final FLASParsingService parsingService;

    public FLASLanguageServer(Repository repo, FLASCompiler compiler) {
    	 parsingService = new FLASParsingService(repo, compiler);
	}
    
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
		return parsingService;
	}

	@Override
	public WorkspaceService getWorkspaceService() {
		return new FLASWorkspaceService();
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
