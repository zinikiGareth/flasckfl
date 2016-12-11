package org.flasck.flas.blockForm;

import java.util.ArrayList;
import java.util.List;

public class Block {
	public ContinuedLine line;
	public final List<Block> nested = new ArrayList<Block>();

	@Deprecated // but the logic that uses this appears to be tangled
	public Block() {
	}
	
	// I think this is the constructor I really want
	public Block(ContinuedLine line) {
		this.line = line;
	}
	
	// constructor for testing purposes
	public Block(int tabIndent, String s) {
		line = new ContinuedLine();
		line.lines.add(new SingleLine("-", 1, new Indent(tabIndent, 0), s));
	}
	
	public boolean isComment() {
		return line == null || line.lines.isEmpty() || line.lines.get(0).indent == null;
	}
	
	@Override
	public String toString() {
		return "Block[" + line + "/"+nested.size()+"]";
	}
}
