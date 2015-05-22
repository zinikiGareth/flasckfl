package org.flasck.flas.tokenizers;

public class KeywordToken {

	public static String from(Tokenizable line) {
		line.skipWS();
		if (!line.hasMore())
			return null;

		int mark = line.at();
		while (line.hasMore() && Character.isLowerCase(line.nextChar())) {
			line.advance();
		}

		return line.fromMark(mark);
	}

}
