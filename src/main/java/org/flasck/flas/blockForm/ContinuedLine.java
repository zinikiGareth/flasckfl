package org.flasck.flas.blockForm;

import java.util.ArrayList;
import java.util.List;

public class ContinuedLine {
	public final List<SingleLine> lines = new ArrayList<SingleLine>();

	public StringBuilder text() {
		StringBuilder sb = new StringBuilder();
		for (SingleLine l : lines) {
			if (sb.length() > 0)
				sb.append("\n");
			sb.append(l.line.trim());
		}
		return sb;
	}

	public InputPosition locationAtText(int pos) {
		int off = 0;
		for (SingleLine l : lines) {
			String trim = l.line.trim();
			if (pos < off + trim.length())
				return new InputPosition(l.uri, l.lineNo, pos-off, l.indent, trim);
			off += trim.length()+1;
		}
		SingleLine l = lines.get(lines.size()-1);
		return new InputPosition(l.uri, l.lineNo, l.line.trim().length(), l.indent, l.line.trim());
	}
	
	@Override
	public String toString() {
		return "CL[" + text() + "]";
	}

	public int actualLine(int pos) {
		int off = 0;
		for (SingleLine l : lines) {
			String trim = l.line.trim();
			if (pos < off + trim.length())
				return l.lineNo;
			off += trim.length()+1;
		}
		return lines.size()-1;
	}
}
