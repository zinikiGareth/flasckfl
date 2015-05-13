package org.flasck.flas.tokenizers;

public class NumberToken {

	public static String from(Tokenizable line) {
		// TODO: this should handle all number formats, e.g.
		// 0
		// 0L
		// 0.3
		// 0.3f
		// 3.3e-6
		line.skipWS();
		int mark = line.at();
		if (!Character.isDigit(line.nextChar()))
			return null;
		line.advance();
		while (line.hasMore() && Character.isDigit(line.nextChar()))
			line.advance();
		return line.fromMark(mark);
	}

}
