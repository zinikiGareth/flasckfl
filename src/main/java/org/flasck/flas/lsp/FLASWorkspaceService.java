package org.flasck.flas.lsp;

import java.util.concurrent.CompletableFuture;

import org.eclipse.lsp4j.DidChangeConfigurationParams;
import org.eclipse.lsp4j.DidChangeWatchedFilesParams;
import org.eclipse.lsp4j.DidChangeWorkspaceFoldersParams;
import org.eclipse.lsp4j.ExecuteCommandParams;
import org.eclipse.lsp4j.services.WorkspaceService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.JsonObject;

public class FLASWorkspaceService implements WorkspaceService {
	private static final Logger logger = LoggerFactory.getLogger("FLASLSP");
	private final LSPCore core;

	public FLASWorkspaceService(LSPCore core) {
		this.core = core;
	}

	@Override
	public void didChangeConfiguration(DidChangeConfigurationParams params) {
		JsonObject settings = (JsonObject) params.getSettings();
		String cardsFolder = settings.get("FLAS").getAsJsonObject().get("lookForCardsInFolder").getAsString();
		core.setCardsFolder(cardsFolder);
	}

	@Override
	public void didChangeWatchedFiles(DidChangeWatchedFilesParams params) {
		logger.info("CHANGE WATCHED FILES");
	}

	@Override
	public CompletableFuture<Object> executeCommand(ExecuteCommandParams params) {
		switch (params.getCommand()) {
		case "flas/readyForNotifications": {
			logger.info("received command readyForNotifications");
			core.doParsing(true);
			return CompletableFuture.completedFuture(null);
		}
		default: {
			logger.error("cannot handle command " + params.getCommand());
			return CompletableFuture.completedFuture(null);
		}
		}
	}

	@Override
	public void didChangeWorkspaceFolders(DidChangeWorkspaceFoldersParams params) {
		logger.info("CHANGE WATCHED FOLDERS");
		WorkspaceService.super.didChangeWorkspaceFolders(params);
	}
	
	
}
