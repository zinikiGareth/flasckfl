package org.flasck.flas.htmlzip;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;
import java.util.zip.ZipInputStream;

public class BuilderSink implements Sink {
	private final Map<String, Block> blocks = new TreeMap<>();
	private final List<Block> fileBlocks = new ArrayList<>();
	private String file;

	@Override
	public void beginFile(String file) {
		this.file = file;
	}

	@Override
	public void card(String tag, int from, int to) {
		if (file == null)
			System.err.println("No current file to handle block " + tag);
		Block curr = findBlockContaining(from, to, false);
		if (curr != null)
			System.err.println("Cannot define block " + tag + " within " + curr);
		Block b = new Block(file, tag, from, to);
		if (blocks.containsKey(tag))
			System.err.println("Multiple definitions for block called " + tag);
		else
			blocks.put(tag, b);
		fileBlocks.add(b);
	}

	@Override
	public void hole(String called, int from, int to) {
		Block b = findBlockContaining(from, to, true);
		if (b != null)
			b.addHole(called, from, to);
	}

	@Override
	public void identifyElement(String called, int from, int to) {
		Block b = findBlockContaining(from, to, true);
		if (b != null)
			b.identify(called, from, to);
	}

	@Override
	public void dodgyAttr(int from, int to) {
		Block b = findBlockContaining(from, to, true);
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

	public void generate(File inf, File jsf) throws IOException {
		PrintWriter pw = new PrintWriter(new FileOutputStream(jsf));
		for (Entry<String, Block> e : blocks.entrySet()) {
			e.getValue().generate(pw, inf);
		}
		pw.close();
	}
	
	private Block findBlockContaining(int from, int to, boolean showErrorOnNone) {
		List<Block> options = new ArrayList<>();
		for (Block b : fileBlocks) {
			if (b.has(from, to))
				options.add(b);
		}
		if (options.size() == 1)
			return options.get(0);
		else if (options.size() > 1)
			System.err.println("There are multiple blocks containing " + from + "-" + to + ":" + options);
		else if (showErrorOnNone)
			System.err.println("There is no block containing " + from + "-" + to);
		return null;
	}
}
