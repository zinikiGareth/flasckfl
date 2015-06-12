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
	public static final int ARROW = 9;
	public static final int ORB = 10;
	public static final int CRB = 11;

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
		} else if (c == '(') {
			line.advance();
			return new TemplateToken(ORB, "(");
		} else if (c == ')') {
			line.advance();
			return new TemplateToken(CRB, ")");
		} else if (c == '=') {
			line.advance();
			if (line.nextChar() == '>') {
				line.advance();
				return new TemplateToken(ARROW, "=>");
			} else
				return new TemplateToken(EQUALS, "=");
		} else
			return null;
	}

	@Override
	public String toString() {
		return "TT[" + type + ":" + text + "]";
	}
}
