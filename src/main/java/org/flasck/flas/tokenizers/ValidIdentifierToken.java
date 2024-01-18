package org.flasck.flas.tokenizers;

import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.errors.ErrorReporter;
import org.flasck.flas.grammar.tracking.LoggableToken;

public class ValidIdentifierToken implements LoggableToken {
	public final InputPosition location;
	public final String text;

	public ValidIdentifierToken(InputPosition pos, String text, int end) {
		this.location = pos;
		this.location.endAt(end);
		this.text = text;
	}

	public static ValidIdentifierToken from(ErrorReporter errors, Tokenizable line) {
		line.skipWS();
		int mark = line.at();
		InputPosition pos = line.realinfo();
		if (!line.hasMore() || !isIdentifierStart(line.nextChar()))
			return null;
		line.advance();
		while (line.hasMore() && isIdentifierPart(line.nextChar()))
			line.advance();
		return errors.logParsingToken(new ValidIdentifierToken(pos, line.fromMark(mark), line.at()));
	}
	
	private static boolean isIdentifierStart(char c) {
		return Character.isLetter(c) || c == '_';
	}

	private static boolean isIdentifierPart(char c) {
		return Character.isLetter(c) || Character.isDigit(c) || c == '_';
	}

	@Override
	public String toString() {
		return "VIT["+text+":" + location+"]";
	}

	@Override
	public InputPosition location() {
		return location;
	}

	@Override
	public String type() {
		return "identifier";
	}

	@Override
	public String text() {
		return text;
	}

}
