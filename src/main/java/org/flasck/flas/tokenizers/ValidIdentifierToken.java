package org.flasck.flas.tokenizers;

import org.flasck.flas.blockForm.InputPosition;

public class ValidIdentifierToken {
	public final InputPosition location;
	public final String text;

	public ValidIdentifierToken(InputPosition pos, String text) {
		this.location = pos;
		this.text = text;
	}

	public static ValidIdentifierToken from(Tokenizable line) {
		line.skipWS();
		int mark = line.at();
		InputPosition pos = line.realinfo();
		if (!line.hasMore() || !Character.isJavaIdentifierStart(line.nextChar()))
			return null;
		line.advance();
		while (line.hasMore() && Character.isJavaIdentifierPart(line.nextChar()))
			line.advance();
		return new ValidIdentifierToken(pos, line.fromMark(mark));
	}

}
