package org.flasck.flas.tokenizers;

import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.errors.ErrorReporter;

public class PattToken {
	public static final int VAR = 1;
	public static final int TYPE = 2;
	public static final int NUMBER = 3;
	public static final int STRING = 4;
	public static final int TRUE = 5;
	public static final int FALSE = 6;

	// chars and strings

	public static final int ORB = 10;
	public static final int CRB = 11;
	public static final int OSB = 12;
	public static final int CSB = 13;
	public static final int OCB = 14;
	public static final int CCB = 15;
	public static final int COLON = 16;
	public static final int COMMA = 17;
	
	public final InputPosition location;
	public final int type;
	public final String text;

	public PattToken(InputPosition loc, int type, String text, int end) {
		this.location = loc;
		this.location.endAt(end);
		this.type = type;
		this.text = text;
	}

	public static PattToken from(ErrorReporter errors, Tokenizable line) {
		line.skipWS();
		if (!line.hasMore())
			return null;
		char c = line.nextChar();
		int pos;
		if (Character.isJavaIdentifierStart(c)) {
			ValidIdentifierToken tok = ValidIdentifierToken.from(errors, line);
			if (tok == null)
				return null;
			if (tok.text.equals("true"))
				return new PattToken(tok.location, PattToken.TRUE, "true", line.at());
			else if (tok.text.equals("false"))
				return new PattToken(tok.location, PattToken.FALSE, "false", line.at());
			else
				return new PattToken(tok.location, Character.isUpperCase(c)?TYPE:VAR, tok.text, line.at());
		} else if (c == '"' || c == '\'') {
			InputPosition loc = line.realinfo();
			String s = StringToken.from(errors, line);
			return new PattToken(loc, PattToken.STRING, s, line.at());
		}
		else if (Character.isDigit(c) || c == '.' && line.still(1) && Character.isDigit(line.charAt(1))) {
			NumberToken num = NumberToken.from(line);
			return new PattToken(num.location, NUMBER, num.text, line.at());
		} else if ((pos = "()[]{}:,".indexOf(c)) != -1) {
			InputPosition loc = line.realinfo();
			line.advance();
			return new PattToken(loc, pos+10, null, line.at());
		} else
			return null;
	}

	@Override
	public String toString() {
		return "PT[" + type + ":" + text + "]";
	}
}
