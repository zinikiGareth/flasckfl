package org.flasck.flas.tokenizers;

import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.errors.ErrorReporter;
import org.flasck.flas.grammar.tracking.LoggableToken;

public class TypeExprToken implements LoggableToken {
	public static final int NAME = 1;

	public static final int ORB = 10;
	public static final int CRB = 11;
	public static final int OSB = 12;
	public static final int CSB = 13;
	public static final int COMMA = 14;
	public static final int ARROW = 15;
	
	public final InputPosition location;
	public final int type;
	public final String text;

	public TypeExprToken(InputPosition location, int type, String text, int end) {
		this.location = location;
		this.location.endAt(end);
		this.type = type;
		this.text = text;
	}

	public static TypeExprToken from(ErrorReporter errors, Tokenizable line) {
		line.skipWS(errors);
		if (!line.hasMore())
			return null;
		InputPosition loc = line.realinfo();
		char c = line.nextChar();
		int pos;
		if (Character.isLetter(c)) {
			TypeNameToken tmp = TypeNameToken.qualified(errors, line);
			if (tmp != null)
				return errors.logParsingToken(new TypeExprToken(loc, NAME, tmp.text, line.at()));
			else
				return null;
		} else if ((pos = "()[],".indexOf(c)) != -1) {
			line.advance();
			return errors.logParsingToken(new TypeExprToken(loc, 10+pos, new String(new char[] { c }), line.at()));
		} else if ("->".equals(line.getTo(2))) {
			return errors.logParsingToken(new TypeExprToken(loc, ARROW, "->", line.at()));
		} else
			return null;
	}

	@Override
	public String toString() {
		return "PT[" + type + ":" + text + "]";
	}

	@Override
	public InputPosition location() {
		return location;
	}

	@Override
	public String type() {
		switch (type) {
		case NAME:
			return "type-name";
		case OSB:
			return "OSB";
		case CSB:
			return "CSB";
		default:
			return "TypeExprToken_" + type;
		}
	}

	@Override
	public String text() {
		return text;
	}
}
