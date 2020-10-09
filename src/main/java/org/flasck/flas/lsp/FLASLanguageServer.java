package org.flasck.flas.lsp;

import java.util.List;
import java.util.concurrent.CompletableFuture;

import org.eclipse.lsp4j.InitializeParams;
import org.eclipse.lsp4j.InitializeResult;
import org.eclipse.lsp4j.ServerCapabilities;
import org.eclipse.lsp4j.TextDocumentSyncKind;
import org.eclipse.lsp4j.WorkspaceFolder;
import org.eclipse.lsp4j.services.LanguageClient;
import org.eclipse.lsp4j.services.LanguageClientAware;
import org.eclipse.lsp4j.services.LanguageServer;
import org.eclipse.lsp4j.services.TextDocumentService;
import org.eclipse.lsp4j.services.WorkspaceService;
import org.flasck.flas.LSPTaskQueue;
import org.flasck.flas.compiler.FLASCompiler;

public class FLASLanguageServer implements LanguageServer, LanguageClientAware {
	private final FLASCompiler compiler;
    private final FLASParsingService parsingService;

	public FLASLanguageServer(FLASCompiler compiler, LSPTaskQueue taskQ) {
		this.compiler = compiler;
		parsingService = new FLASParsingService(compiler, taskQ);
	}

	@Override
	public void connect(LanguageClient client) {
		parsingService.connect(client);
	}

	@Override
	public CompletableFuture<InitializeResult> initialize(InitializeParams params) {
		compiler.addRoot(params.getRootUri());
		List<WorkspaceFolder> folders = params.getWorkspaceFolders();
		if (folders != null) {
			for (WorkspaceFolder f : folders) {
				compiler.addRoot(f.getUri());
			}
		}
        ServerCapabilities capabilities = new ServerCapabilities();
        capabilities.setTextDocumentSync(TextDocumentSyncKind.Full);
        return CompletableFuture.completedFuture(new InitializeResult(capabilities));
	}

	public void setCardsFolder(String cardsFolder) {
		compiler.setCardsFolder(cardsFolder);
	}

	@Override
	public TextDocumentService getTextDocumentService() {
		return parsingService;
	}

	@Override
	public WorkspaceService getWorkspaceService() {
		return new FLASWorkspaceService(this);
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
