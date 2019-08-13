package org.flasck.flas.compiler;

import java.io.File;

public class JSFile {
	private final File file;

	public JSFile(File file) {
		this.file = file;
	}

	public File file() {
		return file;
	}
}
