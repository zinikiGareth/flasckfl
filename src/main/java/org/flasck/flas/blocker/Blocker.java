package org.flasck.flas.blocker;

import org.flasck.flas.blockForm.ContinuedLine;
import org.flasck.flas.blockForm.Indent;
import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.blockForm.SingleLine;
import org.flasck.flas.errors.ErrorReporter;
import org.zinutils.exceptions.UtilException;

public class Blocker {
	private final ErrorReporter errors;
	private final BlockConsumer consumer;
	private ContinuedLine currline;
	private int currLevel = 0;
	
	public Blocker(ErrorReporter errors, BlockConsumer consumer) {
		this.errors = errors;
		this.consumer = consumer;
	}

	public void newFile() {
		consumer.newFile();
	}
	
	public void present(String file, int lineNumber, String text) {
		try {
			consume(file, lineNumber, text);
		} catch (BlockerException ex) {
			errors.message(new InputPosition(file, lineNumber, getIndent(text).tabs, null, text), ex.getMessage());
		}
	}

	private void consume(String file, int lineNumber, String text) {
		Indent ind = getIndent(text);
		text = text.trim();
		InputPosition pos = new InputPosition(file, lineNumber, 0, ind, text);
		if (ind == null || (ind.tabs == 0 && ind.spaces == 0) || text.startsWith("//")) {
			consumer.comment(pos, text);
		} else if (ind.tabs == 0 && ind.spaces != 0) {
			// can't have a line with spaces at start and no tabs
			errors.message(pos, "line cannot start with spaces");
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
			errors.message(pos, "invalid indent");
		} else if (ind.tabs == currLevel+1 && ind.spaces > 0) {
			errors.message(pos, "illegal continuation line (spaces at front)");
		} else {
			// incredibly, we didn't think about this case
			throw new UtilException("We didn't think of that");
		}
	}

	public void flush() {
		if (currline != null) {
			consumer.line(currLevel, currline);
			currLevel = 0;
			currline = null;
		}
		consumer.flush();
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
}
