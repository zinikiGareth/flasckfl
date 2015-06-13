package org.flasck.flas.tokenizers;

import org.flasck.flas.blockForm.InputPosition;

public class KeywordToken {

	public final InputPosition location;
	public final String text;

	public KeywordToken(InputPosition location, String text) {
		this.location = location;
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

		return new KeywordToken(location, line.fromMark(mark));
	}

}
