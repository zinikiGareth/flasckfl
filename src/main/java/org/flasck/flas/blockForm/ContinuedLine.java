package org.flasck.flas.blockForm;

import java.util.ArrayList;
import java.util.List;

public class ContinuedLine {
	public final List<SingleLine> lines = new ArrayList<SingleLine>();

	public StringBuilder text() {
		StringBuilder sb = new StringBuilder();
		for (SingleLine l : lines) {
			if (sb.length() > 0)
				sb.append(" ");
			sb.append(l.line.trim());
		}
		return sb;
	}

	public InputPosition locationAtText(int pos) {
		int off = 0;
		for (SingleLine l : lines) {
			String trim = l.line.trim();
			if (pos < off + trim.length())
				return new InputPosition(l.lineNo, pos-off, trim);
			off += trim.length()+1;
		}
		SingleLine l = lines.get(lines.size()-1);
		return new InputPosition(l.lineNo, l.line.trim().length(), l.line.trim());
	}
	
	@Override
	public String toString() {
		return "CL[" + text() + "]";
	}
}
