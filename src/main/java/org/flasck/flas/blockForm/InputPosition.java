package org.flasck.flas.blockForm;

import org.zinutils.utils.Justification;

public class InputPosition implements Comparable<InputPosition> {
	public final String file;
	public final int lineNo;
	public final int off;
	public final String text;

	public InputPosition(String file, int lineNo, int off, String text) {
		this.file = file;
		this.lineNo = lineNo;
		this.off = off;
		this.text = text;
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
