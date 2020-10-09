package org.flasck.flas.lsp;

import org.eclipse.lsp4j.DidChangeConfigurationParams;
import org.eclipse.lsp4j.DidChangeWatchedFilesParams;
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
		System.out.println("did change config: " + cardsFolder);
		server.setCardsFolder(cardsFolder);
	}

	@Override
	public void didChangeWatchedFiles(DidChangeWatchedFilesParams params) {
	}
}
