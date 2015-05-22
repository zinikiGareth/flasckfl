package org.flasck.flas.tokenizers;

import org.flasck.flas.blockForm.InputPosition;

public class Tokenizable {
	private final StringBuilder input;
	private int pos;

	public Tokenizable(StringBuilder input) {
		this.input = input;
		this.pos = 0;
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
		// TODO Auto-generated method stub
		return null;
	}
	
	@Override
	public String toString() {
		return "Tkz["+input.substring(0, pos)+"__"+input.substring(pos)+"]";
	}
}
