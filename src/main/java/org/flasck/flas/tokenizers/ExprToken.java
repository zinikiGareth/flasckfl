package org.flasck.flas.tokenizers;

public class ExprToken {
	public static final int IDENTIFIER = 1;
	public static final int NUMBER = 2;
	public static final int PUNC = 3;
	public static final int SYMBOL = 4;
	public final int type;
	public final String text;

	public ExprToken(int type, String text) {
		this.type = type;
		this.text = text;
	}

	public static ExprToken from(Tokenizable line) {
		line.skipWS();
		if (!line.hasMore())
			return null;
		int mark = line.at();
		char c = line.nextChar();
		if (Character.isJavaIdentifierStart(c))
			return new ExprToken(IDENTIFIER, ValidIdentifierToken.from(line));
		else if (c == '"' || c == '\'') {
			throw new RuntimeException("Handle string parsing");
		}
		else if (Character.isDigit(c) || c == '.' && line.still(1) && Character.isDigit(line.charAt(1)))
			return new ExprToken(NUMBER, NumberToken.from(line));
		else if ("()[]{}.,".indexOf(c) != -1) {
			line.advance();
			return new ExprToken(PUNC, line.fromMark(mark));
		} else {
			while ("!$%^&|*/+-=:".indexOf(line.nextChar()) != -1) {
				line.advance();
			}
			return new ExprToken(SYMBOL, line.fromMark(mark));
		}
	}

	@Override
	public String toString() {
		return "ET[" + type + ":" + text + "]";
	}
}
