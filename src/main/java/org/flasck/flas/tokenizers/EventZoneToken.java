package org.flasck.flas.tokenizers;

import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.errors.ErrorReporter;
import org.flasck.flas.grammar.tracking.LoggableToken;

public class EventZoneToken implements LoggableToken {
	public static final int NAME = 1;
	public static final int NUMBER = 2;
	public static final int DOT = 3;
	public static final int COLON = 4;
	public static final int CARD = 5;
	public final InputPosition location;
	public final int type;
	public final String text;

	public EventZoneToken(InputPosition location, int type, String text) {
		this.location = location;
		this.type = type;
		this.text = text;
	}

	public static EventZoneToken from(ErrorReporter errors, Tokenizable line) {
		line.skipWS(errors);
		int mark = line.at();
		InputPosition pos = line.realinfo();
		if (!line.hasMore())
			return null;
		if (Character.isLowerCase(line.nextChar())) {
			line.advance();
			char ch;
			while (line.hasMore() && ((ch = line.nextChar()) == '-' || Character.isLowerCase(ch) || Character.isDigit(ch)))
				line.advance();
			return errors.logParsingToken(new EventZoneToken(pos.copySetEnd(line.at()), NAME, line.fromMark(mark)));
		} else if (Character.isDigit(line.nextChar())) {
			line.advance();
			while (line.hasMore() && Character.isDigit(line.nextChar()))
				line.advance();
			return errors.logParsingToken(new EventZoneToken(pos.copySetEnd(line.at()), NUMBER, line.fromMark(mark)));
		} else if (line.nextChar() == '.') {
			line.advance();
			return errors.logParsingToken(new EventZoneToken(pos.copySetEnd(line.at()), DOT, line.fromMark(mark)));
		} else if (line.nextChar() == ':') {
			line.advance();
			return errors.logParsingToken(new EventZoneToken(pos.copySetEnd(line.at()), COLON, line.fromMark(mark)));
		} else if (line.nextChar() == '_') {
			line.advance();
			return errors.logParsingToken(new EventZoneToken(pos.copySetEnd(line.at()), CARD, line.fromMark(mark)));
		} else
			return null;
	}
	
	@Override
	public String toString() {
		return "EZT["+text+":" + location+"]";
	}

	@Override
	public InputPosition location() {
		return location;
	}

	@Override
	public String type() {
		switch (type) {
		case NAME:
			return "zone-name";
		default:
			return "EventZoneToken";
		}
	}

	@Override
	public String text() {
		return text;
	}
}
