package org.flasck.flas.parsedForm;

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
}
