package org.flasck.flas.blockForm;

import org.zinutils.exceptions.UtilException;
import org.zinutils.utils.Justification;

public class InputPosition implements Comparable<InputPosition> {
	public final String file;
	public final int lineNo;
	public final int off;
	private int endPos = -1;
	public transient final String text;

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
		return ret;
	}

	public String asToken() {
		if (endPos < 0)
			return null;
		return text.substring(off, endPos);
	}

	public int pastEnd() {
		if (endPos < 0)
			throw new UtilException("pastEnd called, but not set");
		return endPos;
	}
	
	@Override
	public String toString() {
		return (file != null ? Justification.LEFT.format(file + ": ", 12) : "") + Justification.PADLEFT.format(Integer.toString(lineNo), 3) +"." + off;
	}

	@Override
	public int compareTo(InputPosition o) {
		int ret = file.compareTo(o.file);
		if (ret == 0)
			ret = Integer.compare(lineNo, o.lineNo);
		if (ret == 0)
			ret = Integer.compare(off, o.off);
		return ret;
	}
}
