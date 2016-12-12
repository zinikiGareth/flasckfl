package org.flasck.flas.sampleData;

import java.util.ArrayList;
import java.util.List;

import org.flasck.flas.blockForm.Block;
import org.flasck.flas.blockForm.ContinuedLine;
import org.flasck.flas.blockForm.Indent;
import org.flasck.flas.blockForm.SingleLine;
import org.zinutils.exceptions.UtilException;

public class BlockBuilder {
	private final String file;
	private boolean indent = true;
	private List<Block> stack = new ArrayList<Block>();
	private int lineNo = 1;
	
	public BlockBuilder(String file) {
		this.file = file;
	}
	
	public BlockBuilder lineNo(int n) {
		this.lineNo = n;
		return this;
	}
	
	public BlockBuilder line(String line) {
		Block newBlock = addBlockInAppropriatePlace();
		newBlock.line.lines.add(new SingleLine(file, lineNo++, new Indent(stack.size(), 0), line));
		return this;
	}
	
	private Block addBlockInAppropriatePlace() {
		Block newBlock = new Block();
		if (!indent) {
			stack.remove(stack.size()-1);
			if (stack.isEmpty())
				throw new UtilException("Cannot have multiple top-level blocks");
		}
		if (!stack.isEmpty())
			stack.get(stack.size()-1).nested.add(newBlock);
		stack.add(newBlock);
		indent = false;
		return newBlock;
	}

	public BlockBuilder continuation(int ind, String line) {
		ContinuedLine cl = stack.get(stack.size()-1).line;
		if (cl == null) 
			throw new RuntimeException("cannot continue line before started");
		cl.lines.add(new SingleLine(file, lineNo++, new Indent(stack.size(), ind), line));
		return this;
	}

	public BlockBuilder comment(String line) {
		Block block = addBlockInAppropriatePlace();
		block.line.lines.add(new SingleLine(file, lineNo++, null, line));
		return this;
	}
	
	public BlockBuilder indent() {
		indent = true;
		return this;
	}

	public BlockBuilder exdent() {
		stack.remove(stack.size()-1);
		indent = true;
		return this;
	}

	public Block build() {
		return stack.get(0);
	}
}
