package org.flasck.flas.blocker;

import java.io.IOException;
import java.io.LineNumberReader;
import java.io.Reader;
import java.util.ArrayList;
import java.util.List;

import org.flasck.flas.ErrorResult;
import org.flasck.flas.blockForm.Block;
import org.flasck.flas.blockForm.ContinuedLine;
import org.flasck.flas.blockForm.Indent;
import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.blockForm.SingleLine;
import org.zinutils.collections.CollectionUtils;
import org.zinutils.exceptions.UtilException;

public class Blocker {
	private final ErrorResult errors = new ErrorResult();
	private int lineNo = 0;
	private final List<Block> stack = new ArrayList<Block>();
	
	private Blocker() {
		Block b = new Block(); // a transient block
		stack.add(b);
	}

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
			errors.message(new InputPosition(lineNo, 0, l), "line cannot start with spaces");
		} else if (ind.tabs == cind() && ind.spaces > 0) {
			// this is a continuation line
			stack.get(stack.size()-1).line.lines.add(new SingleLine(lineNo, ind, text));
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
			System.out.println("HANDLE ERROR");
		} else if (ind.tabs == cind()+1 && ind.spaces > 0) {
			// error - first line can't be a continuation line
			System.out.println("HANDLE ERROR");
		} else {
			// incredibly, we didn't think about this case
			throw new UtilException("We didn't think of that");
		}
	}

	private Block pushBlock(Indent ind, String text, boolean addToParent, boolean addToStack) {
		Block b = new Block();
		b.line = new ContinuedLine();
		b.line.lines.add(new SingleLine(lineNo, ind, text));
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

	public static List<Block> block(List<String> lines) {
		Blocker blocker = new Blocker();
		for (String l : lines)
			blocker.accept(l);
		return blocker.stack.get(0).nested;
	}
	
	public static List<Block> block(String input) {
		List<String> lines = CollectionUtils.listOf(input.split("\n"));
		return block(lines);
	}

	public static Object block(Reader reader) {
		LineNumberReader lnr = new LineNumberReader(reader);
		Blocker blocker = new Blocker();
		try {
			String s;
			while ((s = lnr.readLine()) != null)
				blocker.accept(s);
			if (blocker.errors.hasErrors())
				return blocker.errors;
			return blocker.stack.get(0).nested;
		} catch (IOException ex) {
			ex.printStackTrace();
			return ErrorResult.oneMessage(null, "io error: " + ex.getMessage());
		}
	}
}
