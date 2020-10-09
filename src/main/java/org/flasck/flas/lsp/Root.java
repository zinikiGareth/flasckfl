package org.flasck.flas.lsp;

import java.io.File;
import java.net.URI;
import java.util.Iterator;
import java.util.TreeSet;
import java.util.stream.Collectors;

import org.eclipse.lsp4j.MessageParams;
import org.eclipse.lsp4j.MessageType;
import org.eclipse.lsp4j.services.LanguageClient;
import org.zinutils.utils.FileUtils;

public class Root implements Iterable<URI> {
	public final URI uri;
	public final File root;
	private final TreeSet<File> files = new TreeSet<File>(new WorkspaceFileNameComparator());

	public Root(URI uri) {
		this.uri = uri;
		this.root = new File(uri.getPath());
	}

	public void gatherFiles(LanguageClient lsp) {
		files.clear();
		for (File f : FileUtils.findFilesUnderMatching(root, "*")) {
			if (WorkspaceFileNameComparator.isValidExtension(FileUtils.extension((f.getName())))) {
				files.add(f);
			}
		}
		for (File f : files) {
			lsp.logMessage(new MessageParams(MessageType.Log, "gathered " + f));
		}
	}

	@Override
	public Iterator<URI> iterator() {
		return files.stream().map(f -> uri.resolve(f.getPath())).collect(Collectors.toList()).iterator();
	}
}
