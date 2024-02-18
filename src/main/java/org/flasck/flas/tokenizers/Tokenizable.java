package org.flasck.flas.tokenizers;

import org.flasck.flas.blockForm.ContinuedLine;
import org.flasck.flas.blockForm.Indent;
import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.blockForm.SingleLine;
import org.flasck.flas.errors.ErrorReporter;
import org.flasck.flas.errors.ErrorResult;

public class Tokenizable {
	private final ContinuedLine line;
	private final StringBuilder input;
	private int pos;

	public Tokenizable(ContinuedLine l) {
		if (l == null) {
			this.input = null;
			this.line = null;
			return;
		}
		this.input = l.text();
		this.line = l;
	}

	public Tokenizable(ContinuedLine l, StringBuilder in, int pos) {
		this.line = l;
		this.input = in;
		this.pos = pos;
	}

	// This is really only for testing
	public Tokenizable(String input) {
		this.input = new StringBuilder(input);
		this.pos = 0;
		this.line = new ContinuedLine();
		this.line.lines.add(new SingleLine("test", 1, new Indent(1, 0), input));
	}

	public int at() {
		return pos;
	}

	public void skipWS(ErrorReporter errors) {
		while (pos < input.length() && Character.isWhitespace(input.charAt(pos)))
			pos++;
		if (pos+1 < input.length() && input.charAt(pos) == '/' && input.charAt(pos+1) == '/') {
			InputPosition loc = realinfo();
			int at = pos;
			int idx = input.indexOf("\n", pos);
			if (idx == -1)
				pos = input.length();
			else
				pos = idx+1;
			errors.logParsingToken(new CommentToken(loc, input.substring(at, pos).trim()));
		}
	}

	public boolean still(int i) {
		return pos+i < input.length();
	}

	public boolean hasMore() {
		return pos < input.length();
	}
	
	public boolean hasMoreContent(ErrorReporter errors) {
		skipWS(errors);
		return pos < input.length();
	}
	
	public char nextChar() {
		return input.charAt(pos);
	}

	public char charAt(int i) {
		return input.charAt(pos+i);
	}

	public void advance() {
		pos++;
	}

	public String fromMark(int mark) {
		if (pos <= mark)
			return null;
		return input.substring(mark, pos);
	}

	public String remainder() {
		return input.substring(pos, input.length());
	}
	
	public String getTo(int length) {
		if (pos+length > input.length())
			length = input.length()-pos;
		if (length <= 0)
			return null;
		String ret = input.substring(pos, pos+length);
		pos += length;
		return ret;
	}

	public void reset(int mark) {
		pos = mark;
	}

	public InputPosition realinfo() {
		if (line == null)
			return null;
		return line.locationAtText(pos);
	}

	public InputPosition locationAtText(int i) {
		if (line == null)
			return null;
		return line.locationAtText(i);
	}
	
	@Override
	public String toString() {
		return "Tkz["+input.substring(0, pos)+"__"+input.substring(pos)+"]";
	}

	public int length() {
		return input.length();
	}

	public int actualLine() {
		return line.actualLine(pos);
	}

	public int find(String sub) {
		ErrorResult tmp = new ErrorResult();
		int init = pos;
		for (int i=pos;i<input.length();i++) {
			if (input.substring(i).startsWith("//"))
				return -1;
			if (input.charAt(i) == '"' || input.charAt(i) == '\'') {
				pos = i;
				StringToken.from(tmp, this);
				i = pos;
				pos = init;
			}
			if (input.substring(i).startsWith(sub))
				return i;
		}
		return -1;
	}

	public Tokenizable cropAt(int k) {
		StringBuilder in2 = new StringBuilder(input.substring(0, k));
		return new Tokenizable(line, in2, pos);
	}

	public Tokenizable copyTo(String string) {
		int k = find("->");
		if (k == -1) {
			return this;
		} else
			return cropAt(k);
	}
}
