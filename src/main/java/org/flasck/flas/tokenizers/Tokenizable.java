package org.flasck.flas.tokenizers;

import org.flasck.flas.blockForm.Block;
import org.flasck.flas.blockForm.ContinuedLine;
import org.flasck.flas.blockForm.InputPosition;

public class Tokenizable {
	private final ContinuedLine line;
	private final StringBuilder input;
	private int pos;

	public Tokenizable(Block b) {
		this(b.line);
	}
	
	public Tokenizable(ContinuedLine l) {
		this.input = l.text();
		this.line = l;
	}

	// This is really only for testing
	public Tokenizable(String input) {
		this.input = new StringBuilder(input);
		this.pos = 0;
		this.line = null;
	}

	public int at() {
		return pos;
	}

	public void skipWS() {
		while (pos < input.length() && Character.isWhitespace(input.charAt(pos)))
			pos++;
	}

	public boolean still(int i) {
		return pos+i < input.length();
	}

	public boolean hasMore() {
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
	
	@Override
	public String toString() {
		return "Tkz["+input.substring(0, pos)+"__"+input.substring(pos)+"]";
	}
}
