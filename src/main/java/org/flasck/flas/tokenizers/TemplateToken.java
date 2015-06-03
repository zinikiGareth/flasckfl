package org.flasck.flas.tokenizers;

public class TemplateToken {
	public static final int IDENTIFIER = 1;
	public static final int STRING = 2;
	public static final int COLON = 3;
	public static final int DIV = 4;
	public static final int LIST = 5;
	public static final int HASH = 6;
	public static final int ATTR = 7;
	public static final int EQUALS = 8;

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
			return new TemplateToken(STRING, StringToken.from(line));
		} else if (c == '.') {
			line.advance();
			return new TemplateToken(DIV, ".");
		} else if (c == '+') {
			line.advance();
			return new TemplateToken(LIST, "+");
		} else if (c == ':') {
			line.advance();
			return new TemplateToken(COLON, ":");
		} else if (c == '#') {
			line.advance();
			return new TemplateToken(HASH, "#");
		} else if (c == '@') {
			line.advance();
			return new TemplateToken(ATTR, "@");
		} else if (c == '=') {
			line.advance();
			return new TemplateToken(EQUALS, "=");
		} else
			return null;
	}

	@Override
	public String toString() {
		return "TT[" + type + ":" + text + "]";
	}
}
