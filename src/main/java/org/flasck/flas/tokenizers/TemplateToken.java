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
	public static final int OR = 16;
	public static final int CASES = 17;
	public static final int EDITABLE = 18;
	public static final int WEBZIP = 19;
	public static final int ASSIGN = 20;

	public final InputPosition location;
	public final int type;
	public final String text;

	public TemplateToken(InputPosition location, int type, String text, int end) {
		this.location = location;
		this.location.endAt(end);
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
			return new TemplateToken(vit.location, IDENTIFIER, vit.text, line.at());
		} else if (c == '"' || c == '\'') {
			return new TemplateToken(loc, STRING, StringToken.from(line), line.at());
		} else if (c == '.') {
			line.advance();
			return new TemplateToken(loc, DIV, ".", line.at());
		} else if (c == '+') {
			line.advance();
			return new TemplateToken(loc, LIST, "+", line.at());
		} else if (c == '|') {
			line.advance();
			if (line.hasMore() && line.nextChar() == '|') {
				line.advance();
				return new TemplateToken(loc, CASES, "||", line.at());
			}
			return new TemplateToken(loc, OR, "|", line.at());
		} else if (c == ':') {
			line.advance();
			return new TemplateToken(loc, COLON, ":", line.at());
		} else if (c == '#') {
			line.advance();
			return new TemplateToken(loc, HASH, "#", line.at());
		} else if (c == '@') {
			line.advance();
			return new TemplateToken(loc, ATTR, "@", line.at());
		} else if (c == '%') {
			line.advance();
			return new TemplateToken(loc, WEBZIP, "%", line.at());
		} else if (c == '$') {
			line.advance();
			line.skipWS();
			ValidIdentifierToken tok = VarNameToken.from(line);
			if (tok == null)
				return null;
			return new TemplateToken(loc, TEMPLATE, tok.text, line.at());
		} else if (c == '>') {
			line.advance();
			return new TemplateToken(loc, CARD, ">", line.at());
		} else if (c == '(') {
			line.advance();
			return new TemplateToken(loc, ORB, "(", line.at());
		} else if (c == ')') {
			line.advance();
			return new TemplateToken(loc, CRB, ")", line.at());
		} else if (c == '=') {
			line.advance();
			if (line.nextChar() == '>') {
				line.advance();
				return new TemplateToken(loc, ARROW, "=>", line.at());
			} else
				return new TemplateToken(loc, EQUALS, "=", line.at());
		} else if (c == '<') {
			int pos = line.at();
			line.advance();
			if (line.nextChar() == '-') {
				line.advance();
				return new TemplateToken(loc, ASSIGN, "<-", line.at());
			} else {
				line.reset(pos);
				return null;
			}
		} else if (c == '?') {
			line.advance();
			return new TemplateToken(loc, EDITABLE, "?", line.at());
		} else
			return null;
	}

	@Override
	public String toString() {
		return "TT[" + type + ":" + text + "]";
	}
}
