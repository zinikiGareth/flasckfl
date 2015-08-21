package org.flasck.flas.blockForm;


public class SingleLine {
	public final String file;
	public final int lineNo;
	public Indent indent; // is a comment if indent is null
	public String line;
	
	public SingleLine(String file, int lineNo, Indent indent, String line) {
		this.file = file;
		this.lineNo = lineNo;
		this.indent = indent;
		this.line = line;
	}
}
