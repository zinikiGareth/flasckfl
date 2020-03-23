package org.flasck.flas.tokenizers;

import org.flasck.flas.blockForm.InputPosition;

public class KeywordToken {

	public final InputPosition location;
	public final String text;

	public KeywordToken(InputPosition location, String text, int end) {
		this.location = location;
		location.endAt(end);
		this.text = text;
	}

	public static KeywordToken from(Tokenizable line) {
		line.skipWS();
		if (!line.hasMore())
			return null;

		InputPosition location = line.realinfo();
		int mark = line.at();
		while (line.hasMore() && Character.isLowerCase(line.nextChar())) {
			line.advance();
		}
		if (line.hasMore() && !Character.isWhitespace(line.nextChar())) {
			line.reset(mark);
			return null;
		}

		String ret = line.fromMark(mark);
		if (ret == null)
			return null;
		return new KeywordToken(location, ret, line.at());
	}

	@Override
	public String toString() {
		return "KW[" + text + "]";
	}
}
