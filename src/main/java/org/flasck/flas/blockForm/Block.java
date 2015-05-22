package org.flasck.flas.blockForm;

import java.util.ArrayList;
import java.util.List;

public class Block {
	public ContinuedLine line;
	public final List<Block> nested = new ArrayList<Block>();
	
	public boolean isComment() {
		return line == null || line.lines.isEmpty() || line.lines.get(0).indent == null;
	}
}
