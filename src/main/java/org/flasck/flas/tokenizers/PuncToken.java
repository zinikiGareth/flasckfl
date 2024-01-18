package org.flasck.flas.tokenizers;

import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.errors.ErrorReporter;
import org.flasck.flas.grammar.tracking.LoggableToken;

public class PuncToken implements LoggableToken {
	public final InputPosition location;
	public final String text;

	public PuncToken(InputPosition location, String text) {
		this.location = location;
		this.text = text;
	}

	public static PuncToken from(ErrorReporter errors, Tokenizable line) {
		line.skipWS(errors);
		if (!line.hasMore())
			return null;
		InputPosition loc = line.realinfo();
		int mark = line.at();
		char c = line.nextChar();
		if ("()[]{}.,:".indexOf(c) != -1) {
			line.advance();
			return errors.logParsingToken(new PuncToken(loc.copySetEnd(line.at()), line.fromMark(mark)));
		} else {
			line.reset(mark);
			return null;
		}
	}

	@Override
	public String toString() {
		return "PT[" + text + "]";
	}

	@Override
	public InputPosition location() {
		return location;
	}

	@Override
	public String type() {
		return "Punctoken";
	}

	@Override
	public String text() {
		// TODO Auto-generated method stub
		return text;
	}
}
