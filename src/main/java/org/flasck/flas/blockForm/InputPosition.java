package org.flasck.flas.blockForm;

import org.zinutils.exceptions.UtilException;
import org.zinutils.utils.Justification;

public class InputPosition implements Comparable<InputPosition> {
	public final String file;
	public final int lineNo;
	public final int off;
	private int endPos = -1;
	public transient final String text;
	private boolean isFakeToken;

	public InputPosition(String file, int lineNo, int off, String text) {
		this.file = file;
		this.lineNo = lineNo;
		this.off = off;
		this.text = text;
	}

	public void endAt(int end) {
		if (end == -1) // don't override potentially valid value with dumb one
			return;
		this.endPos = end;
	}

	public InputPosition copySetEnd(int at) {
		InputPosition ret = new InputPosition(file, lineNo, off, text);
		ret.endAt(at);
		ret.isFakeToken = isFakeToken;
		return ret;
	}

	public String asToken() {
		if (endPos < 0)
			return null;
		return text.substring(off, endPos);
	}

	public boolean hasEnd() {
		return endPos >=0;
	}
	
	public int pastEnd() {
		if (endPos < 0)
			throw new UtilException("pastEnd called, but not set");
		return endPos;
	}
	
	@Override
	public String toString() {
		return (file != null ? Justification.LEFT.format(file + ": ", 12) : "") + Justification.PADLEFT.format(Integer.toString(lineNo), 3) +"." + off/* + "  " + pastEnd() + " " + isFakeToken*/; 
	}

	@Override
	public boolean equals(Object obj) {
		return obj instanceof InputPosition && this.compareTo((InputPosition) obj) == 0;
	}
	@Override
	public int compareTo(InputPosition o) {
		if (o == null)
			return -1;
		int ret = 0;
		if (file != null)
			ret = file.compareTo(o.file);
		if (ret == 0)
			ret = Integer.compare(lineNo, o.lineNo);
		if (ret == 0)
			ret = Integer.compare(off, o.off);
		if (ret == 0)
			ret = Integer.compare(endPos, o.endPos);
		return ret;
	}

	public InputPosition lesserOf(InputPosition p) {
		if (p == null || this.compareTo(p) <= 0)
			return this;
		return p;
	}

	public InputPosition fakeToken() {
		this.isFakeToken = true;
		return this;
	}
	
	public boolean isFake() {
		return isFakeToken;
	}

	public String inFile() {
		return Integer.toString(lineNo) + ":" + Integer.toString(off);
	}
}
