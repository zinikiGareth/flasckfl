package org.flasck.flas.tokenizers;

import org.flasck.flas.blockForm.InputPosition;

public class ValidIdentifierToken {
	public final InputPosition location;
	public final String text;

	public ValidIdentifierToken(InputPosition pos, String text, int end) {
		this.location = pos;
		this.location.endAt(end);
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
		return new ValidIdentifierToken(pos, line.fromMark(mark), line.at());
	}
	
	@Override
	public String toString() {
		return "VIT["+text+":" + location+"]";
	}

}
