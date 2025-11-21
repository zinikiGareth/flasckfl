package org.flasck.flas.blockForm;

import java.io.File;
import java.net.URI;

public class SingleLine {
	public final URI uri;
	public final String file;
	public final int lineNo;
	public Indent indent; // is a comment if indent is null
	public String line;
	
	public SingleLine(URI uri, int lineNo, Indent indent, String line) {
		this.uri = uri;
		this.file = new File(uri.getPath()).getName();
		this.lineNo = lineNo;
		this.indent = indent;
		this.line = line;
	}
}
