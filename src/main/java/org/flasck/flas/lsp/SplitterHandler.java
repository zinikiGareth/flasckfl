package org.flasck.flas.lsp;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.lsp4j.Diagnostic;
import org.eclipse.lsp4j.PublishDiagnosticsParams;
import org.eclipse.lsp4j.services.LanguageClient;
import org.flasck.flas.errors.FLASError;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SplitterHandler extends DiagnosticHandler implements LSPErrorHandler {
	private static final Logger logger = LoggerFactory.getLogger("FLASLSP");
	private final LanguageClient client;
	private final URI processingUri;
	private final List<Diagnostic> diagnostics = new ArrayList<>();

	public SplitterHandler(LanguageClient client, URI uri) {
		this.client = client;
		this.processingUri = uri;
	}

	@Override
	public void handle(FLASError e) {
		logger.info("saw error: " + e + " on " + processingUri);
        Diagnostic diagnostic = makeDiagnostic(processingUri, e);
        diagnostics.add(diagnostic);
	}

	@Override
	public void done(List<URI> broken) {
		if (diagnostics.isEmpty())
			broken.remove(processingUri);
		else
			broken.add(processingUri);
		logger.info("done processing " + processingUri + ": sending " + diagnostics.size() + " diagnostics");
		synchronized (client) {
			client.publishDiagnostics(new PublishDiagnosticsParams(processingUri.toString(), diagnostics));
		}
	}
	
	@Override
	public int errorCount() {
		return diagnostics.size();
	}

}
