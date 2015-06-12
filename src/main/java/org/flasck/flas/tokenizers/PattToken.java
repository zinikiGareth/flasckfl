package org.flasck.flas.tokenizers;

public class PattToken {
	public static final int VAR = 1;
	public static final int TYPE = 2;
	public static final int NUMBER = 3;
	public static final int TRUE = 4;
	public static final int FALSE = 5;

	// chars and strings

	public static final int ORB = 10;
	public static final int CRB = 11;
	public static final int OSB = 12;
	public static final int CSB = 13;
	public static final int OCB = 14;
	public static final int CCB = 15;
	public static final int COLON = 16;
	public static final int COMMA = 17;
	
	public final int type;
	public final String text;

	public PattToken(int type, String text) {
		this.type = type;
		this.text = text;
	}

	public static PattToken from(Tokenizable line) {
		line.skipWS();
		if (!line.hasMore())
			return null;
		char c = line.nextChar();
		int pos;
		if (Character.isJavaIdentifierStart(c)) {
			String tok = ValidIdentifierToken.from(line);
			if (tok.equals("true"))
				return new PattToken(PattToken.TRUE, "true");
			else if (tok.equals("false"))
				return new PattToken(PattToken.FALSE, "false");
			else
				return new PattToken(Character.isUpperCase(c)?TYPE:VAR, tok);
		} else if (c == '"' || c == '\'') {
			throw new RuntimeException("Handle string parsing");
		}
		else if (Character.isDigit(c) || c == '.' && line.still(1) && Character.isDigit(line.charAt(1)))
			return new PattToken(NUMBER, NumberToken.from(line));
		else if ((pos = "()[]{}:,".indexOf(c)) != -1) {
			line.advance();
			return new PattToken(pos+10, null);
		} else
			return null;
	}

	@Override
	public String toString() {
		return "PT[" + type + ":" + text + "]";
	}
}
