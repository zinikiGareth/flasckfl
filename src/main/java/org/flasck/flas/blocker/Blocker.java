package org.flasck.flas.blocker;

import java.io.IOException;
import java.io.LineNumberReader;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.flasck.flas.blockForm.Block;
import org.flasck.flas.blockForm.ContinuedLine;
import org.flasck.flas.blockForm.Indent;
import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.blockForm.SingleLine;
import org.flasck.flas.errors.ErrorReporter;
import org.flasck.flas.errors.ErrorResult;
import org.zinutils.exceptions.UtilException;

public class Blocker {
	@Deprecated
	private final String file;
	private final ErrorReporter errors;
	private final BlockConsumer consumer;
	@Deprecated
	private int lineNo = 0;
	@Deprecated
	private final List<Block> stack = new ArrayList<Block>();
	private ContinuedLine currline;
	private int currLevel = 0;
	
	@Deprecated
	private Blocker(String file) {
		this.errors = new ErrorResult();
		this.file = file;
		this.consumer = null;
		Block b = new Block(); // a transient block
		stack.add(b);
	}

	public Blocker(ErrorReporter errors, BlockConsumer consumer) {
		this.file = null;
		this.errors = errors;
		this.consumer = consumer;
	}

	public void newFile() {
		stack.clear();
		Block b = new Block(); // a transient block
		stack.add(b);
		consumer.newFile();
	}
	
	public void present(String file, int lineNumber, String text) {
		try {
			consume(file, lineNumber, text);
		} catch (BlockerException ex) {
			errors.message(new InputPosition(file, lineNumber, getIndent(text).tabs, text), ex.getMessage());
		}
	}

	private void consume(String file, int lineNumber, String text) {
		Indent ind = getIndent(text);
//			System.out.println(lineNo + " " + ind + ": " + l);
		text = text.trim();
		if (ind == null || (ind.tabs == 0 && ind.spaces == 0)) {
			consumer.comment(text);
		} else if (ind.tabs == 0 && ind.spaces != 0) {
			// can't have a line with spaces at start and no tabs
			errors.message(new InputPosition(file, lineNumber, 0, text), "line cannot start with spaces");
		} else if (ind.tabs == currLevel  && ind.spaces > 0) {
			// this is a continuation line
			currline.lines.add(new SingleLine(file, lineNumber, ind, text));
		} else if (ind.tabs <= currLevel+1 && ind.spaces == 0) {
			// a new line either at some valid level of scoping (less, same, just one more)
			if (currline != null)
				consumer.line(currLevel, currline);
			currline = new ContinuedLine();
			currline.lines.add(new SingleLine(file, lineNumber, ind, text));
			currLevel = ind.tabs;
		} else if (ind.tabs > currLevel+1) {
			errors.message(new InputPosition(file, lineNumber, 0, text), "invalid indent");
		} else if (ind.tabs == currLevel+1 && ind.spaces > 0) {
			errors.message(new InputPosition(file, lineNumber, 0, text), "illegal continuation line (spaces at front)");
		} else {
			// incredibly, we didn't think about this case
			throw new UtilException("We didn't think of that");
		}
	}

	public void flush() {
		if (currline != null)
			consumer.line(currLevel, currline);
	}

	@Deprecated
	private int cind() {
		return stack.size()-1;
	}

	private void accept(String l) {
		lineNo++;
		Indent ind = getIndent(l);
//		System.out.println(lineNo + " " + ind + ": " + l);
		String text = l.trim();
		if (ind == null || (ind.tabs == 0 && ind.spaces == 0)) {
			// this is a comment
			pushBlock(null, text, true, false);
		} else if (ind.tabs == 0 && ind.spaces != 0) {
			// can't have a line with spaces at start and no tabs
			errors.message(new InputPosition(file, lineNo, 0, l), "line cannot start with spaces");
		} else if (ind.tabs == cind() && ind.spaces > 0) {
			// this is a continuation line
			stack.get(stack.size()-1).line.lines.add(new SingleLine(file, lineNo, ind, text));
		} else if (ind.tabs == cind()+1 && ind.spaces == 0) {
			// this is an indented line
			pushBlock(ind, text, true, true);
		} else if (ind.tabs == cind() && ind.spaces == 0) {
			// this is a new declaration at the same level of definition
			stack.remove(ind.tabs);
			pushBlock(ind, text, true, true);
		} else if (ind.tabs < cind()) {
			// we have closed one or more scopes
			while (stack.size() > ind.tabs)
				stack.remove(ind.tabs);
			pushBlock(ind, text, true, true);
		} else if (ind.tabs > cind()+1) {
			// error - indenting too much
			throw new BlockerException("excess indent");
		} else if (ind.tabs == cind()+1 && ind.spaces > 0) {
			throw new BlockerException("illegal continuation line (spaces at front)");
		} else {
			// incredibly, we didn't think about this case
			throw new UtilException("We didn't think of that");
		}
	}

	private Block pushBlock(Indent ind, String text, boolean addToParent, boolean addToStack) {
		Block b = new Block();
		b.line.lines.add(new SingleLine(file, lineNo, ind, text));
		if (addToParent)
			stack.get(stack.size()-1).nested.add(b);
		if (addToStack)
			stack.add(b);
		return b;
	}

	private Indent getIndent(String l) {
		int tabs = 0;
		int spaces = 0;
		
		while (tabs < l.length() && l.charAt(tabs) == '\t')
			tabs++;
		while (tabs+spaces < l.length() && l.charAt(tabs+spaces) == ' ')
			spaces++;
		
		if (tabs+spaces < l.length())
			return new Indent(tabs, spaces);
		else
			return null;
	}

	@Deprecated
	public static List<Block> block(List<String> lines) {
		Blocker blocker = new Blocker("unknown");
		for (String l : lines)
			blocker.accept(l);
		return blocker.stack.get(0).nested;
	}
	
	@Deprecated
	public static List<Block> block(String input) {
		List<String> lines = Arrays.asList(input.split("\n"));
		return block(lines);
	}

	@Deprecated
	public static Object block(String file, Reader reader) {
		LineNumberReader lnr = new LineNumberReader(reader);
		Blocker blocker = new Blocker(file);
		try {
			String s;
			while ((s = lnr.readLine()) != null) {
				try {
					blocker.accept(s);
				} catch (BlockerException ex) {
					blocker.errors.message(new InputPosition(file, lnr.getLineNumber(), blocker.getIndent(s).tabs, s), ex.getMessage());
				}
			}
			if (blocker.errors.hasErrors())
				return blocker.errors;
			return blocker.stack.get(0).nested;
		} catch (IOException ex) {
			ex.printStackTrace();
			return ErrorResult.oneMessage(new InputPosition(blocker.file, lnr.getLineNumber(), 0, "<io error>"), "io error: " + ex.getMessage());
		}
	}
}
