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
		// TODO: should we allow qualifiers such as
		// 0L
		// 0.3f
		line.skipWS();
		int mark = line.at();
		InputPosition pos = line.realinfo();
		while (line.hasMore() && Character.isDigit(line.nextChar()))
			line.advance();
		// allow exactly one '.'
		if (line.hasMore() && line.nextChar() == '.') {
			line.advance(); // over the '.'
			// and now allow more numbers
			while (line.hasMore() && Character.isDigit(line.nextChar()))
				line.advance();
		}
		// allow a trailing exponent
		int m1 = line.at();
		if (line.hasMore() && line.nextChar() == 'e') {
			line.advance(); // over the 'e'
			if (line.hasMore() && line.nextChar() == '-')
				line.advance(); // a negative exponent is ok
			if (!line.hasMore() || !Character.isDigit(line.nextChar()))
				return new NumberToken(pos, line.fromMark(mark), m1); // only up until before the 'e'
			while (line.hasMore() && Character.isDigit(line.nextChar()))
				line.advance();
		}
		return new NumberToken(pos, line.fromMark(mark), line.at());
	}

}
