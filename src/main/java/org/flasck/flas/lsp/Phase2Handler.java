package org.flasck.flas.lsp;

import java.net.URI;
import java.util.ArrayList;
import java.util.Iterator;
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
	private final ListMap<URI, Diagnostic> diagnostics = new ListMap<>();

	public Phase2Handler(LanguageClient client) {
		this.client = client;
	}

	@Override
	public void handle(FLASError e) {
		logger.info("handling phase 2 error " + e);
        Diagnostic diagnostic = makeDiagnostic(e.getUri(), e);
        diagnostics.add(e.getUri(), diagnostic);
	}

	@Override
	public void done(List<URI> broken) {
		for (URI uri : diagnostics.keySet()) {
			List<Diagnostic> report = diagnostics.get(uri);
			if (!report.isEmpty()) {
				logger.info("reporting phase 2 diagnostics for " + uri + ": " + report.size());
				broken.add(uri);
				synchronized (client) {
					client.publishDiagnostics(new PublishDiagnosticsParams(uri.toString(), report));
				}
			}
		}
		Iterator<URI> it = broken.iterator();
		while (it.hasNext()) {
			URI uri = it.next();
			if (!diagnostics.contains(uri)) {
				it.remove();
				synchronized (client) {
					client.publishDiagnostics(new PublishDiagnosticsParams(uri.toString(), new ArrayList<>()));
				}
			}
		}
	}

	@Override
	public int errorCount() {
		return diagnostics.totalSize();
	}
}
