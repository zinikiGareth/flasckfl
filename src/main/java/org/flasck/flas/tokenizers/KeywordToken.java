package org.flasck.flas.tokenizers;

public class KeywordToken {

	public static String from(Tokenizable line) {
		line.skipWS();
		if (!line.hasMore())
			return null;

		int mark = line.at();
		char c = line.nextChar();
		while (line.hasMore() && Character.isLowerCase(c)) {
			line.advance();
			c = line.nextChar();
		}

		return line.fromMark(mark);
	}

}
