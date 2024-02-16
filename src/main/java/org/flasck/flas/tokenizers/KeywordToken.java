package org.flasck.flas.tokenizers;

import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.errors.ErrorReporter;
import org.flasck.flas.grammar.tracking.LoggableToken;

public class KeywordToken implements LoggableToken {
	public final InputPosition location;
	public final String text;

	public KeywordToken(InputPosition location, String text, int end) {
		this.location = location;
		location.endAt(end);
		this.text = text;
	}

	public static KeywordToken from(ErrorReporter errors, Tokenizable line) {
		line.skipWS(errors);
		if (!line.hasMore())
			return null;

		InputPosition location = line.realinfo();
		int mark = line.at();
		while (line.hasMore() && Character.isLowerCase(line.nextChar())) {
			line.advance();
		}
		if (line.hasMore() && !Character.isWhitespace(line.nextChar())) {
			line.reset(mark);
			return null;
		}

		String ret = line.fromMark(mark);
		if (ret == null)
			return null;
		return errors.logParsingToken(new KeywordToken(location, ret, line.at()));
	}

	@Override
	public InputPosition location() {
		return location;
	}

	@Override
	public String type() {
		return "keyword";
	}

	@Override
	public String text() {
		return text;
	}

	@Override
	public String toString() {
		return "KW[" + text + "]";
	}
}
