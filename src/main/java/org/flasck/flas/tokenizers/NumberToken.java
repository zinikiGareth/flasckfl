package org.flasck.flas.tokenizers;

import org.flasck.flas.blockForm.InputPosition;

public class NumberToken {
	public final InputPosition location;
	public final String text;

	public NumberToken(InputPosition pos, String text, int end) {
		this.location = pos;
		this.location.endAt(end);
		this.text = text;
	}

	public static NumberToken from(Tokenizable line) {
		// TODO: this should handle all number formats, e.g.
		// 0
		// 0L
		// 0.3
		// 0.3f
		// 3.3e-6
		line.skipWS();
		int mark = line.at();
		InputPosition pos = line.realinfo();
		if (!Character.isDigit(line.nextChar()))
			return null;
		line.advance();
		while (line.hasMore() && Character.isDigit(line.nextChar()))
			line.advance();
		return new NumberToken(pos, line.fromMark(mark), line.at());
	}

}
