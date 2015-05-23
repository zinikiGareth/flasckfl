package org.flasck.flas.tokenizers;

public class MessageToken {
	public static final int IDENTIFIER = 1;
	public static final int DOT = 2;
	public static final int ARROW = 3;
	public final int type;
	public final String text;

	public MessageToken(int type, String text) {
		this.type = type;
		this.text = text;
	}

	public static MessageToken from(Tokenizable line) {
		line.skipWS();
		if (!line.hasMore())
			return null;
		char c = line.nextChar();
		if (Character.isJavaIdentifierStart(c))
			return new MessageToken(IDENTIFIER, ValidIdentifierToken.from(line));
		else if (c == '.') {
			line.advance();
			return new MessageToken(DOT, ".");
		} else if ("<-".equals(line.getTo(2))) {
			return new MessageToken(ARROW, "<-");
		} else
			return null;
	}

	@Override
	public String toString() {
		return "MT[" + type + ":" + text + "]";
	}

}
