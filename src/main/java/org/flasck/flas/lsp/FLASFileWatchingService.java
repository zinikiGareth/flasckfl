package org.flasck.flas.lsp;

import java.net.URI;
import java.net.URISyntaxException;

import org.eclipse.lsp4j.DidChangeTextDocumentParams;
import org.eclipse.lsp4j.DidCloseTextDocumentParams;
import org.eclipse.lsp4j.DidOpenTextDocumentParams;
import org.eclipse.lsp4j.DidSaveTextDocumentParams;
import org.eclipse.lsp4j.TextDocumentContentChangeEvent;
import org.eclipse.lsp4j.services.TextDocumentService;
import org.flasck.flas.errors.ErrorReporter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.zinutils.utils.FileUtils;

public class FLASFileWatchingService implements TextDocumentService {
	private static final Logger logger = LoggerFactory.getLogger("FLASLSP");
	private final LSPCore dispatcher;
	private final ErrorReporter errors;

	public FLASFileWatchingService(ErrorReporter errors, LSPCore dispatcher) {
		this.errors = errors;
		this.dispatcher = dispatcher;
	}

	@Override
	public void didOpen(DidOpenTextDocumentParams params) {
    	URI uri = parseURI(params.getTextDocument().getUri());
    	if (uri == null) {
    		logger.warn("no uri specified");
    		return;
    	}
    	String text = params.getTextDocument().getText();
    	logger.info("saw open of " + uri);
    	dispatcher.dispatch(uri, text);
	}

	@Override
	public void didChange(DidChangeTextDocumentParams params) {
    	URI uri = parseURI(params.getTextDocument().getUri());
    	if (uri == null) {
    		logger.warn("no uri specified");
    		return;
    	}
    	logger.info("saw change to " + uri);
    	if (!WorkspaceFileNameComparator.isValidExtension(FileUtils.extension((uri.getPath()))))
    		return;
        for (TextDocumentContentChangeEvent changeEvent : params.getContentChanges()) {
            // Will be full update because we specified that is all we support
            if (changeEvent.getRange() != null) {
                throw new UnsupportedOperationException("Range should be null for full document update.");
            }
            if (changeEvent.getText() == null) {
                throw new UnsupportedOperationException("Text should not be null.");
            }

        	dispatcher.dispatch(uri, changeEvent.getText());
        }
	}

	@Override
	public void didSave(DidSaveTextDocumentParams params) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void didClose(DidCloseTextDocumentParams params) {
		System.out.println("CLOSE");
	}

	private URI parseURI(String uris) {
    	try {
    		return new URI(uris);
    	} catch (URISyntaxException ex) {
            errors.logMessage("Problem parsing " + uris);
            return null;
    	}
	}
}
