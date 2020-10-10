package org.flasck.flas.lsp;

import java.util.List;
import java.util.concurrent.CompletableFuture;

import org.eclipse.lsp4j.DidChangeConfigurationParams;
import org.eclipse.lsp4j.DidChangeWatchedFilesParams;
import org.eclipse.lsp4j.DidChangeWorkspaceFoldersParams;
import org.eclipse.lsp4j.ExecuteCommandParams;
import org.eclipse.lsp4j.SymbolInformation;
import org.eclipse.lsp4j.WorkspaceSymbolParams;
import org.eclipse.lsp4j.services.WorkspaceService;

import com.google.gson.JsonObject;

public class FLASWorkspaceService implements WorkspaceService {
	private final FLASLanguageServer server;

	public FLASWorkspaceService(FLASLanguageServer server) {
		this.server = server;
	}

	@Override
	public void didChangeConfiguration(DidChangeConfigurationParams params) {
		JsonObject settings = (JsonObject) params.getSettings();
		String cardsFolder = settings.get("FLAS").getAsJsonObject().get("lookForCardsInFolder").getAsString();
		server.setCardsFolder(cardsFolder);
	}

	@Override
	public void didChangeWatchedFiles(DidChangeWatchedFilesParams params) {
		System.out.println("CHANGE WATCHED FILES");
	}

	@Override
	public CompletableFuture<Object> executeCommand(ExecuteCommandParams params) {
		System.out.println("COMMAND");
		return WorkspaceService.super.executeCommand(params);
	}

	@Override
	public CompletableFuture<List<? extends SymbolInformation>> symbol(WorkspaceSymbolParams params) {
		System.out.println("SYMBOL");
		return WorkspaceService.super.symbol(params);
	}

	@Override
	public void didChangeWorkspaceFolders(DidChangeWorkspaceFoldersParams params) {
		System.out.println("CHANGE WATCHED FOLDERS");
		WorkspaceService.super.didChangeWorkspaceFolders(params);
	}
	
	
}
