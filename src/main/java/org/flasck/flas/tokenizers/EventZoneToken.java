package org.flasck.flas.tokenizers;

import org.flasck.flas.blockForm.InputPosition;

public class EventZoneToken {
	public static final int NAME = 1;
	public static final int NUMBER = 2;
	public static final int DOT = 3;
	public static final int CARD = 4;
	public final InputPosition location;
	public final int type;
	public final String text;

	public EventZoneToken(InputPosition location, int type, String text) {
		this.location = location;
		this.type = type;
		this.text = text;
	}

	public static EventZoneToken from(Tokenizable line) {
		line.skipWS();
		int mark = line.at();
		InputPosition pos = line.realinfo();
		if (!line.hasMore())
			return null;
		if (Character.isLowerCase(line.nextChar())) {
			line.advance();
			char ch;
			while (line.hasMore() && ((ch = line.nextChar()) == '-' || Character.isLowerCase(ch) || Character.isDigit(ch)))
				line.advance();
			return new EventZoneToken(pos.copySetEnd(line.at()), NAME, line.fromMark(mark));
		} else if (Character.isDigit(line.nextChar())) {
			line.advance();
			while (line.hasMore() && Character.isDigit(line.nextChar()))
				line.advance();
			return new EventZoneToken(pos.copySetEnd(line.at()), NUMBER, line.fromMark(mark));
		} else if (line.nextChar() == '.') {
			line.advance();
			return new EventZoneToken(pos.copySetEnd(line.at()), DOT, line.fromMark(mark));
		} else if (line.nextChar() == '_') {
			line.advance();
			return new EventZoneToken(pos.copySetEnd(line.at()), CARD, line.fromMark(mark));
		} else
			return null;
	}
	
	@Override
	public String toString() {
		return "EZT["+text+":" + location+"]";
	}
}
