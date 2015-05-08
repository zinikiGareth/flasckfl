package org.flasck.flas.parser;

import java.util.ArrayList;
import java.util.List;

import org.flasck.flas.parsedForm.Block;
import org.flasck.flas.parsedForm.ContinuedLine;
import org.flasck.flas.parsedForm.SingleLine;
import org.zinutils.collections.CollectionUtils;
import org.zinutils.exceptions.UtilException;

public class Blocker {
	private int lineNo = 0;
	private final List<FLASError> errors = new ArrayList<FLASError>();
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
		String text = l.trim();
		if (ind.tabs == 0 && ind.spaces == 0) {
			System.out.println("Adding comment " + text);
			// this is a comment
			pushBlock(null, text, true, false);
		} else if (ind.tabs == cind() && ind.spaces > 0) {
			System.out.println("Adding continuation line " + text);
			// this is a continuation line
			stack.get(stack.size()-1).line.lines.add(new SingleLine(lineNo, ind, text));
		} else if (ind.tabs == cind()+1 && ind.spaces == 0) {
			System.out.println("Adding indented line " + text);
			// this is an indented line
			pushBlock(ind, text, true, true);
		} else if (ind.tabs == cind() && ind.spaces == 0) {
			System.out.println("Adding same line " + text);
			// this is a new declaration at the same level of definition
			stack.remove(ind.tabs);
			pushBlock(ind, text, true, true);
		} else if (ind.tabs < cind()) {
			System.out.println("Exdenting");
			// we have closed one or more scopes
			while (stack.size() > ind.tabs)
				stack.remove(ind.tabs);
			pushBlock(ind, text, true, true);
		} else if (ind.tabs > cind()+1) {
			// error - indenting too much
			System.out.println("error");
		} else if (ind.tabs == cind()+1 && ind.spaces > 0) {
			// error - first line can't be a continuation line
			System.out.println("error");
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
		
		while (l.charAt(tabs) == '\t')
			tabs++;
		while (l.charAt(tabs+spaces) == ' ')
			spaces++;
		
		return new Indent(tabs, spaces);
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
}
