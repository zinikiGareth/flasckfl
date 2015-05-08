package org.flasck.flas.parsedForm;

import java.util.ArrayList;
import java.util.List;

public class Block {
	public ContinuedLine line;
	public final List<Block> nested = new ArrayList<Block>();
}
