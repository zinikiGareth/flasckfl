package org.flasck.flas.tokenizers;

import org.flasck.flas.blockForm.InputPosition;

public class MessageToken {
	public static final int IDENTIFIER = 1;
	public static final int DOT = 2;
	public static final int ARROW = 3;

	public final InputPosition location;
	public final int type;
	public final String text;

	public MessageToken(InputPosition loc, int type, String text, int end) {
		location = loc;
		location.endAt(end);
		this.type = type;
		this.text = text;
	}

	public static MessageToken from(Tokenizable line) {
		line.skipWS();
		if (!line.hasMore())
			return null;
		InputPosition loc = line.realinfo();
		char c = line.nextChar();
		if (Character.isJavaIdentifierStart(c))
			return new MessageToken(loc, IDENTIFIER, ValidIdentifierToken.from(line).text, line.at());
		else if (c == '.') {
			line.advance();
			return new MessageToken(loc, DOT, ".", line.at());
		} else if ("<-".equals(line.getTo(2))) {
			return new MessageToken(loc, ARROW, "<-", line.at());
		} else
			return null;
	}

	@Override
	public String toString() {
		return "MT[" + type + ":" + text + "]";
	}

}
