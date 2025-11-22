package org.flasck.flas.lsp;

import java.util.concurrent.CompletableFuture;

import org.eclipse.lsp4j.DeleteFilesParams;
import org.eclipse.lsp4j.DidChangeConfigurationParams;
import org.eclipse.lsp4j.DidChangeWatchedFilesParams;
import org.eclipse.lsp4j.DidChangeWorkspaceFoldersParams;
import org.eclipse.lsp4j.ExecuteCommandParams;
import org.eclipse.lsp4j.services.WorkspaceService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FLASWorkspaceService implements WorkspaceService {
	private static final Logger logger = LoggerFactory.getLogger("FLASLSP");
	private final LSPCore core;

	public FLASWorkspaceService(LSPCore core) {
		this.core = core;
	}

	@Override
	public void didChangeConfiguration(DidChangeConfigurationParams params) {
		logger.info("CHANGE CONFIGURATION called");
	}

	@Override
	public void didChangeWorkspaceFolders(DidChangeWorkspaceFoldersParams params) {
		logger.info("CHANGE WATCHED FOLDERS");
		WorkspaceService.super.didChangeWorkspaceFolders(params);
	}

	@Override
	public void didChangeWatchedFiles(DidChangeWatchedFilesParams params) {
		logger.info("CHANGE WATCHED FILES");
	}
	
	@Override
	public void didDeleteFiles(DeleteFilesParams params) {
		logger.info("DELETED FILES");
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
}
