package org.flasck.flas.lsp;

import java.io.File;
import java.net.URI;
import java.util.TreeSet;

import org.eclipse.lsp4j.MessageParams;
import org.eclipse.lsp4j.MessageType;
import org.eclipse.lsp4j.services.LanguageClient;
import org.zinutils.utils.FileUtils;

public class Root {
	private final LanguageClient client;
	private final Submitter submitter;
	private final URI uri;
	public final File root;
	private final TreeSet<File> files = new TreeSet<File>(new WorkspaceFileNameComparator());

	public Root(LanguageClient client, Submitter submitter, URI uri) {
		this.client = client;
		this.submitter = submitter;
		this.uri = uri;
		this.root = new File(uri.getPath());
		client.logMessage(new MessageParams(MessageType.Log, "opening root " + root));
	}

	public void gatherFiles() {
		files.clear();
		for (File f : FileUtils.findFilesMatching(root, "*")) {
			if (WorkspaceFileNameComparator.find(FileUtils.extension((f.getName()))) != -1) {
				files.add(f);
			}
		}
		for (File f : files) {
			client.logMessage(new MessageParams(MessageType.Log, "gathered " + FileUtils.makeRelativeTo(f, root)));
		}
		compileAll();
	}

	private void compileAll() {
		for (File f : files) {
			submitter.submit(compile(f));
		}
	}

	private CompileFile compile(File f) {
		switch (FileUtils.extension(f.getName())) {
		case ".fl":
			return new CompileFLAS(uri.resolve(f.getPath()));
		case ".ut":
			break;
		case ".st":
			break;
		case ".fa":
			return new CompileFA(uri.resolve(f.getPath()));
		default:
			client.logMessage(new MessageParams(MessageType.Log, "could not compile " + FileUtils.makeRelativeTo(f, root)));
		}
		return null;
	}
}
