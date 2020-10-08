package org.flasck.flas.lsp;

import java.io.File;
import java.net.URI;

public abstract class AbstractCompilation implements CompileFile {
	protected URI uri;
	protected final File file;
	protected final String inPkg;
	protected final String name;

	public AbstractCompilation(URI uri) {
		this.uri = uri;
		this.file = new File(uri.getPath());
		this.name = file.getName();
		File dir = file.getParentFile();
		this.inPkg = dir.getName();
	}
}
