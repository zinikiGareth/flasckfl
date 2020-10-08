package org.flasck.flas.lsp;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Executor;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.eclipse.lsp4j.DidChangeTextDocumentParams;
import org.eclipse.lsp4j.DidCloseTextDocumentParams;
import org.eclipse.lsp4j.DidOpenTextDocumentParams;
import org.eclipse.lsp4j.DidSaveTextDocumentParams;
import org.eclipse.lsp4j.MessageParams;
import org.eclipse.lsp4j.MessageType;
import org.eclipse.lsp4j.TextDocumentContentChangeEvent;
import org.eclipse.lsp4j.services.LanguageClient;
import org.eclipse.lsp4j.services.TextDocumentService;
import org.flasck.flas.compiler.FLASCompiler;
import org.flasck.flas.repository.Repository;

public class FLASParsingService implements TextDocumentService {
	private final BlockingQueue<Runnable> tasks = new LinkedBlockingQueue<Runnable>();
	private final Executor exec = new ThreadPoolExecutor(1, 1, 0L, TimeUnit.SECONDS, tasks);
	private final Map<String, Root> roots = new TreeMap<>();
	private final CompilationSubmitter submitter;
	private LanguageClient client;

	public FLASParsingService(LSPErrorForwarder errors, Repository repository, FLASCompiler compiler) {
		this.submitter = new CompilationSubmitter(errors, repository, tasks, exec);
	}

	public void connect(LanguageClient client) {
		this.client = client;
		this.submitter.connect(client);
	}

	public void addRoot(String rootUri) {
		try {
			URI uri = new URI(rootUri + "/");
			Root root = new Root(client, submitter, uri);
			if (roots.containsKey(root.root.getPath()))
				return;
			roots.put(root.root.getPath(), root);
			root.gatherFiles();
		} catch (URISyntaxException ex) {
			client.logMessage(new MessageParams(MessageType.Error, "could not open " + rootUri));
		}
	}

	@Override
	public void didOpen(DidOpenTextDocumentParams params) {
    	URI uri = parseURI(params.getTextDocument().getUri());
    	if (uri == null)
    		return;
    	String text = params.getTextDocument().getText();
    	System.out.println("saw open of " + uri);
    	parse(uri, text);
	}

	@Override
	public void didChange(DidChangeTextDocumentParams params) {
    	URI uri = parseURI(params.getTextDocument().getUri());
    	if (uri == null)
    		return;
    	System.out.println("saw change to " + uri);
        for (TextDocumentContentChangeEvent changeEvent : params.getContentChanges()) {
            // Will be full update because we specified that is all we support
            if (changeEvent.getRange() != null) {
                throw new UnsupportedOperationException("Range should be null for full document update.");
            }
            if (changeEvent.getText() == null) {
                throw new UnsupportedOperationException("Text should not be null.");
            }

            parse(uri, changeEvent.getText());
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

	private void parse(URI uri, String text) {
		// repository.clean(uri);
    	String path = uri.getPath();
    	for (Entry<String, Root> e : roots.entrySet()) {
    		if (path.startsWith(e.getKey())) {
    			System.out.println("matched root " + e.getKey());
				e.getValue().parse(uri, text);
    			return;
    		}
    	}
    	client.logMessage(new MessageParams(MessageType.Warning, "did not find a root for " + uri));
	}
}
