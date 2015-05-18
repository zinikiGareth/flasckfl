package org.flasck.flas.blockForm;


public class SingleLine {
	public int lineNo;
	public Indent indent; // is a comment if indent is null
	public String line;
	
	public SingleLine(int lineNo, Indent indent, String line) {
		this.lineNo = lineNo;
		this.indent = indent;
		this.line = line;
	}
}
