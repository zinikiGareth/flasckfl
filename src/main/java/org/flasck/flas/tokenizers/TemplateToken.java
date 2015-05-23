package org.flasck.flas.tokenizers;

public class TemplateToken {
	public static final int IDENTIFIER = 1;
	public static final int COLON = 2;
	public static final int HASH = 3;

	public final int type;
	public final String text;

	public TemplateToken(int type, String text) {
		this.type = type;
		this.text = text;
	}

	public static TemplateToken from(Tokenizable line) {
		line.skipWS();
		if (!line.hasMore())
			return null;
		
		char c = line.nextChar();
		if (Character.isLowerCase(c) && Character.isJavaIdentifierStart(c))
			return new TemplateToken(IDENTIFIER, ValidIdentifierToken.from(line));
		else if (c == '"' || c == '\'') {
			throw new RuntimeException("Handle string parsing");
		} else if (c == ':') {
			line.advance();
			return new TemplateToken(COLON, ":");
		} else if (c == '#') {
			line.advance();
			return new TemplateToken(HASH, "#");
		} else
			return null;
	}

	@Override
	public String toString() {
		return "TT[" + type + ":" + text + "]";
	}
}
