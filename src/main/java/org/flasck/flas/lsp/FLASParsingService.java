package org.flasck.flas.lsp;

import java.io.File;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Set;
import java.util.TreeSet;

import org.eclipse.lsp4j.DidChangeTextDocumentParams;
import org.eclipse.lsp4j.DidCloseTextDocumentParams;
import org.eclipse.lsp4j.DidOpenTextDocumentParams;
import org.eclipse.lsp4j.DidSaveTextDocumentParams;
import org.eclipse.lsp4j.MessageParams;
import org.eclipse.lsp4j.MessageType;
import org.eclipse.lsp4j.services.LanguageClient;
import org.eclipse.lsp4j.services.TextDocumentService;
import org.flasck.flas.compiler.FLASCompiler;
import org.flasck.flas.repository.Repository;
import org.zinutils.utils.FileUtils;

public class FLASParsingService implements TextDocumentService {
	private final Repository repository;
	private final FLASCompiler compiler;
	private LanguageClient client;
	private URI uri;
	private File root;
	private Set<File> files;

	public FLASParsingService(Repository repository, FLASCompiler compiler) {
		this.repository = repository;
		this.compiler = compiler;
	}

	public void connect(LanguageClient client) {
		this.client = client;
	}

	public void setWorkspaceRoot(String rootUri) {
		try {
			uri = new URI(rootUri + "/");
			root = new File(uri.getPath());
			gatherFiles();
		} catch (URISyntaxException ex) {
			uri = null;
			root = null;
		}
	}

	private void gatherFiles() {
		files = new TreeSet<File>(new WorkspaceFileNameComparator());
		for (File f : FileUtils.findFilesMatching(root, "*")) {
			if (WorkspaceFileNameComparator.find(FileUtils.extension((f.getName()))) != -1) {
				files.add(f);
			}
		}
		for (File f : files) {
			client.logMessage(new MessageParams(MessageType.Log, "gathered " + FileUtils.makeRelativeTo(f, root)));
		}
	}

	@Override
	public void didOpen(DidOpenTextDocumentParams params) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void didChange(DidChangeTextDocumentParams params) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void didClose(DidCloseTextDocumentParams params) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void didSave(DidSaveTextDocumentParams params) {
		// TODO Auto-generated method stub
		
	}
}
