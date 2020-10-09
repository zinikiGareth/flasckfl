package org.flasck.flas.lsp;

import java.net.URI;
import java.net.URISyntaxException;

import org.eclipse.lsp4j.DidChangeTextDocumentParams;
import org.eclipse.lsp4j.DidCloseTextDocumentParams;
import org.eclipse.lsp4j.DidOpenTextDocumentParams;
import org.eclipse.lsp4j.DidSaveTextDocumentParams;
import org.eclipse.lsp4j.MessageParams;
import org.eclipse.lsp4j.MessageType;
import org.eclipse.lsp4j.TextDocumentContentChangeEvent;
import org.eclipse.lsp4j.services.LanguageClient;
import org.eclipse.lsp4j.services.TextDocumentService;
import org.flasck.flas.compiler.CompileUnit;
import org.flasck.flas.compiler.TaskQueue;
import org.zinutils.utils.FileUtils;

public class FLASParsingService implements TextDocumentService {
	private final CompileUnit compiler;
	private final TaskQueue queue;
	private LanguageClient client;

	public FLASParsingService(CompileUnit compiler, TaskQueue queue) {
		this.compiler = compiler;
		this.queue = queue;
	}

	public void connect(LanguageClient client) {
		this.client = client;
	}

	@Override
	public void didOpen(DidOpenTextDocumentParams params) {
    	URI uri = parseURI(params.getTextDocument().getUri());
    	if (uri == null)
    		return;
    	String text = params.getTextDocument().getText();
    	System.out.println("saw open of " + uri);
    	if (WorkspaceFileNameComparator.isValidExtension(FileUtils.extension((uri.getPath()))))
    		queue.submit(new CompileTask(compiler, uri, text));
	}

	@Override
	public void didChange(DidChangeTextDocumentParams params) {
    	URI uri = parseURI(params.getTextDocument().getUri());
    	if (uri == null)
    		return;
    	System.out.println("saw change to " + uri);
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

            queue.submit(new CompileTask(compiler, uri, changeEvent.getText()));
        }
	}

	@Override
	public void didSave(DidSaveTextDocumentParams params) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void didClose(DidCloseTextDocumentParams params) {
		// TODO Auto-generated method stub
		
	}

	private URI parseURI(String uris) {
    	try {
    		return new URI(uris);
    	} catch (URISyntaxException ex) {
            client.logMessage(new MessageParams(MessageType.Warning, "Problem parsing " + uris));
            return null;
    	}
	}
}
