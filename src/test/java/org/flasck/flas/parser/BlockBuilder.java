package org.flasck.flas.parser;

import java.util.ArrayList;
import java.util.List;

import org.flasck.flas.parsedForm.Block;
import org.flasck.flas.parsedForm.ContinuedLine;
import org.flasck.flas.parsedForm.SingleLine;

public class BlockBuilder {
	private Block block = new Block();
	private List<Block> stack = new ArrayList<Block>();
	private int lineNo = 1;
	
	public BlockBuilder() {
		stack.add(block);
	}
	
	public BlockBuilder line(String line) {
		ContinuedLine cl = stack.get(stack.size()-1).line;
		if (cl == null) 
			cl = stack.get(stack.size()-1).line = new ContinuedLine();
		cl.lines.add(new SingleLine(lineNo++, new Indent(stack.size(), cl.lines.size()), line));
		return this;
	}
	
	public BlockBuilder indent() {
		Block b = new Block();
		stack.get(stack.size()-1).nested.add(b);
		stack.add(b);
		return this;
	}

	public Block build() {
		return block;
	}
}
