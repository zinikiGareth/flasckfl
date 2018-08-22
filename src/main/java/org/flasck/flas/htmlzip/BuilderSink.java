package org.flasck.flas.htmlzip;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;

public class BuilderSink implements Sink {
	private final Map<String, Block> blocks = new TreeMap<>();
	private final List<Block> fileBlocks = new ArrayList<>();
	private File fromZip;
	private String file;

	@Override
	public void zipLocation(File fromZip) {
		this.fromZip = fromZip;
	}

	@Override
	public void beginFile(String file) {
		this.file = file;
	}

	@Override
	public void card(String tag, int from, int to) {
		if (file == null)
			throw new SplitterException("No current file to handle block " + tag);
		checkNoBlockContaining(tag, from, to);
		Block b = new Block(fromZip, file, tag, from, to);
		if (blocks.containsKey(tag))
			System.err.println("Multiple definitions for block called " + tag);
		else
			blocks.put(tag, b);
		fileBlocks.add(b);
	}

	@Override
	public void holeid(String called, int from, int to) {
		Block b = findUniqueBlockContaining(from, to);
		if (b != null)
			b.addHoleId(called, from, to);
	}

	@Override
	public void hole(int from, int to) {
		Block b = findUniqueBlockContaining(from, to);
		if (b != null)
			b.addHole(from, to);
	}

	@Override
	public void identityAttr(String called, int from, int to) {
		List<Block> bs = findBlocksContaining(from, to);
		for (Block b : bs)
			b.identityAttr(called, from, to);
	}

	@Override
	public void dodgyAttr(int from, int to) {
		Block b = findUniqueBlockContaining(from, to);
		if (b != null)
			b.removeAttr(from, to);
	}

	@Override
	public void fileEnd() {
		this.file = null;
		this.fileBlocks.clear();
	}
	
	public void dump() {
		for (Entry<String, Block> e : blocks.entrySet()) {
			System.out.println(e.getKey());
			e.getValue().dump();
		}
	}

	private Block findUniqueBlockContaining(int from, int to) {
		List<Block> options = findBlocksContaining(from, to);
		if (options.size() == 1)
			return options.get(0);
		
		System.out.println("Error processing " + fromZip + " " + file);
		System.out.println("Found blocks at " + from + "-" + to + ": " + options);
		System.out.println("All blocks: " + blocks);
		if (options.size() > 1)
			throw new SplitterException("There are multiple blocks containing " + from + "-" + to + ":" + options);
		else
			throw new SplitterException("There is no block containing " + from + "-" + to);
	}

	private List<Block> findBlocksContaining(int from, int to) {
		List<Block> options = new ArrayList<>();
		for (Block b : fileBlocks) {
			if (b.has(from, to))
				options.add(b);
		}
		return options;
	}

	private void checkNoBlockContaining(String tag, int from, int to) {
		List<Block> options = findBlocksContaining(from, to);
		if (options.size() > 0)
			throw new SplitterException("Cannot define block " + tag + " within " + options.get(0));
	}

	public void visitCard(String string, CardVisitor visitor) {
		if (!blocks.containsKey(string))
			throw new SplitterException("There is no block " + string);
		blocks.get(string).visit(visitor);
	}

	public Block getBlock(String called) {
		return blocks.get(called);
	}
}
