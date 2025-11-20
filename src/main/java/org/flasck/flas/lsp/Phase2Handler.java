package org.flasck.flas.lsp;

import java.net.URI;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.eclipse.lsp4j.Diagnostic;
import org.eclipse.lsp4j.PublishDiagnosticsParams;
import org.eclipse.lsp4j.services.LanguageClient;
import org.flasck.flas.errors.FLASError;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.zinutils.collections.ListMap;

public class Phase2Handler extends DiagnosticHandler implements LSPErrorHandler {
	private static final Logger logger = LoggerFactory.getLogger("FLASLSP");
	private final LanguageClient client;
	private final URI workspaceUri;
	private final ListMap<URI, Diagnostic> diagnostics = new ListMap<>();

	public Phase2Handler(LanguageClient client, URI workspace) {
		this.client = client;
		this.workspaceUri = workspace;
	}

	@Override
	public void handle(FLASError e) {
		logger.info("handling phase 2 error " + e);
        Diagnostic diagnostic = makeDiagnostic(workspaceUri, e);
        URI uri = workspaceUri;
        if (e.loc != null && e.loc.file != null) {
        	uri = workspaceUri.resolve(e.loc.file); 
        }
        diagnostics.add(uri, diagnostic);
	}

	@Override
	public void done(List<URI> broken) {
		for (URI uri : diagnostics.keySet()) {
			List<Diagnostic> report = diagnostics.get(uri);
			if (!report.isEmpty()) {
				logger.info("reporting phase 2 diagnostics for " + uri);
				broken.add(uri);
				synchronized (client) {
					client.publishDiagnostics(new PublishDiagnosticsParams(uri.toString(), report));
				}
			}
		}
		synchronized (client) {
			List<Diagnostic> projDiag = new ArrayList<>();
			logger.info("done processing " + workspaceUri + ": sending " + projDiag.size() + " diagnostics");
			client.publishDiagnostics(new PublishDiagnosticsParams(workspaceUri.toString(), projDiag));
		}
	}

	@Override
	public int errorCount() {
		return diagnostics.totalSize();
	}
}
