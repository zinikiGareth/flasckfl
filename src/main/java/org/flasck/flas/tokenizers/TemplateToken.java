package org.flasck.flas.tokenizers;

import org.flasck.flas.blockForm.InputPosition;

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
	public static final int TEMPLATE = 12;
	public static final int CARD = 13;
	// PUNNET = 14
	// CHOICEPUNNET = 15
	// OR = 16
	// CASES = 17

	public final InputPosition location;
	public final int type;
	public final String text;

	public TemplateToken(InputPosition location, int type, String text) {
		this.location = location;
		this.type = type;
		this.text = text;
	}

	public static TemplateToken from(Tokenizable line) {
		line.skipWS();
		if (!line.hasMore())
			return null;
		
		InputPosition loc = line.realinfo();
		char c = line.nextChar();
		if (Character.isLowerCase(c) && Character.isJavaIdentifierStart(c)) {
			ValidIdentifierToken vit = ValidIdentifierToken.from(line);
			if (vit == null)
				return null;
			return new TemplateToken(loc, IDENTIFIER, vit.text);
		} else if (c == '"' || c == '\'') {
			return new TemplateToken(loc, STRING, StringToken.from(line));
		} else if (c == '.') {
			line.advance();
			return new TemplateToken(loc, DIV, ".");
		} else if (c == '+') {
			line.advance();
			return new TemplateToken(loc, LIST, "+");
		} else if (c == ':') {
			line.advance();
			return new TemplateToken(loc, COLON, ":");
		} else if (c == '#') {
			line.advance();
			return new TemplateToken(loc, HASH, "#");
		} else if (c == '@') {
			line.advance();
			return new TemplateToken(loc, ATTR, "@");
		} else if (c == '$') {
			line.advance();
			line.skipWS();
			ValidIdentifierToken tok = VarNameToken.from(line);
			if (tok == null)
				return null;
			return new TemplateToken(loc, TEMPLATE, tok.text);
		} else if (c == '>') {
			line.advance();
			return new TemplateToken(loc, CARD, ">");
		} else if (c == '(') {
			line.advance();
			return new TemplateToken(loc, ORB, "(");
		} else if (c == ')') {
			line.advance();
			return new TemplateToken(loc, CRB, ")");
		} else if (c == '=') {
			line.advance();
			if (line.nextChar() == '>') {
				line.advance();
				return new TemplateToken(loc, ARROW, "=>");
			} else
				return new TemplateToken(loc, EQUALS, "=");
		} else
			return null;
	}

	@Override
	public String toString() {
		return "TT[" + type + ":" + text + "]";
	}
}
