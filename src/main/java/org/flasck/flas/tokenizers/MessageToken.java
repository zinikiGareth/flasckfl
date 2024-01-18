package org.flasck.flas.tokenizers;

import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.errors.ErrorReporter;
import org.flasck.flas.grammar.tracking.LoggableToken;

public class MessageToken implements LoggableToken {
	public static final int IDENTIFIER = 1;
	public static final int DOT = 2;
	public static final int ARROW = 3;

	public final InputPosition location;
	public final int type;
	public final String text;

	public MessageToken(InputPosition loc, int type, String text, int end) {
		location = loc;
		location.endAt(end);
		this.type = type;
		this.text = text;
	}

	public static MessageToken from(ErrorReporter errors, Tokenizable line) {
		line.skipWS();
		if (!line.hasMore())
			return null;
		InputPosition loc = line.realinfo();
		char c = line.nextChar();
		if (Character.isJavaIdentifierStart(c))
			return errors.logParsingToken(new MessageToken(loc, IDENTIFIER, ValidIdentifierToken.from(errors, line).text, line.at()));
		else if (c == '.') {
			line.advance();
			return errors.logParsingToken(new MessageToken(loc, DOT, ".", line.at()));
		} else if ("<-".equals(line.getTo(2))) {
			return errors.logParsingToken(new MessageToken(loc, ARROW, "<-", line.at()));
		} else
			return null;
	}

	@Override
	public String toString() {
		return "MT[" + type + ":" + text + "]";
	}

	@Override
	public InputPosition location() {
		return location;
	}

	@Override
	public String type() {
		return "Message_"+type;
	}

	@Override
	public String text() {
		return text;
	}

}
