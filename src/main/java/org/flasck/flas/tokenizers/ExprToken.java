package org.flasck.flas.tokenizers;

import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.errors.ErrorReporter;

public class ExprToken {
	public static final int IDENTIFIER = 1;
	public static final int NUMBER = 2;
	public static final int PUNC = 3;
	public static final int SYMBOL = 4;
	public static final int STRING = 5;

	public final InputPosition location;
	public final int type;
	public final String text;

	public ExprToken(InputPosition location, int type, String text) {
		this.location = location;
		this.type = type;
		this.text = text;
	}

	public ExprToken(int type, ValidIdentifierToken vit) {
		this.location = vit.location;
		this.type = type;
		this.text = vit.text;
	}

	public ExprToken(int type, NumberToken from) {
		this.location = from.location;
		this.type = type;
		this.text = from.text;
	}

	public static ExprToken from(ErrorReporter errors, Tokenizable line) {
		line.skipWS();
		if (!line.hasMore())
			return null;
		InputPosition loc = line.realinfo();
		int mark = line.at();
		char c = line.nextChar();
		if (Character.isJavaIdentifierStart(c))
			return new ExprToken(IDENTIFIER, ValidIdentifierToken.from(line));
		else if (c == '"' || c == '\'') {
			String tok = StringToken.from(errors, line);
			if (tok == null)
				return null;
			return new ExprToken(loc.copySetEnd(line.at()), STRING, tok);
		}
		else if (Character.isDigit(c) || c == '.' && line.still(1) && Character.isDigit(line.charAt(1)))
			return new ExprToken(NUMBER, NumberToken.from(line));
		else if ("()[]{}.,:".indexOf(c) != -1) {
			line.advance();
			return new ExprToken(loc.copySetEnd(line.at()), PUNC, line.fromMark(mark));
		} else {
			while (line.hasMore() && "~!$%^&|*/+-=:<>".indexOf(line.nextChar()) != -1) {
				line.advance();
			}
			if (line.at() == mark)
				return null;
			return new ExprToken(loc.copySetEnd(line.at()), SYMBOL, line.fromMark(mark));
		}
	}

	@Override
	public String toString() {
		return "ET[" + type + ":" + text + "]";
	}
}
