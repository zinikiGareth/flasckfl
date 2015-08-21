package org.flasck.flas.sampleData;

import java.util.ArrayList;
import java.util.List;

import org.flasck.flas.blockForm.Block;
import org.flasck.flas.blockForm.ContinuedLine;
import org.flasck.flas.blockForm.Indent;
import org.flasck.flas.blockForm.SingleLine;

public class BlockBuilder {
	private final String file;
	private Block block = new Block();
	private List<Block> stack = new ArrayList<Block>();
	private int lineNo = 1;
	
	public BlockBuilder(String file) {
		this.file = file;
		stack.add(block);
	}
	
	public BlockBuilder lineNo(int n) {
		this.lineNo = n;
		return this;
	}
	
	public BlockBuilder line(String line) {
		ContinuedLine cl = stack.get(stack.size()-1).line;
		if (cl == null)
			cl = stack.get(stack.size()-1).line = new ContinuedLine();
		else {
			stack.remove(stack.size()-1);
			Block b = new Block();
			stack.get(stack.size()-1).nested.add(b);
			stack.add(b);
			cl = new ContinuedLine();
			b.line = cl;
		}
		cl.lines.add(new SingleLine(file, lineNo++, new Indent(stack.size(), 0), line));
		return this;
	}
	
	public BlockBuilder continuation(int ind, String line) {
		ContinuedLine cl = stack.get(stack.size()-1).line;
		if (cl == null) 
			throw new RuntimeException("cannot continue line before started");
		cl.lines.add(new SingleLine(file, lineNo++, new Indent(stack.size(), ind), line));
		return this;
	}

	public BlockBuilder comment(String line) {
		ContinuedLine cl = stack.get(stack.size()-1).line;
		if (cl != null) { 
			Block b = new Block();
			stack.get(stack.size()-1).nested.add(b);
			cl = new ContinuedLine();
			b.line = cl;
		} else
			cl = stack.get(stack.size()-1).line = new ContinuedLine();
		cl.lines.add(new SingleLine(file, lineNo++, null, line));
		return this;
	}
	
	public BlockBuilder indent() {
		Block b = new Block();
		stack.get(stack.size()-1).nested.add(b);
		stack.add(b);
		return this;
	}

	public BlockBuilder exdent() {
		stack.remove(stack.size()-1);
		stack.get(stack.size()-1).nested.add(new Block());
		return this;
	}

	public Block build() {
		return block;
	}
}
