package org.flasck.flas.tokenizers;

import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.errors.ErrorReporter;
import org.flasck.flas.grammar.tracking.LoggableToken;

public class TemplateNameToken implements LoggableToken {
	public final InputPosition location;
	public final String text;

	public TemplateNameToken(InputPosition location, String text) {
		this.location = location;
		this.text = text;
	}

	public static TemplateNameToken from(ErrorReporter errors, Tokenizable line) {
		line.skipWS(errors);
		int mark = line.at();
		InputPosition pos = line.realinfo();
		if (!line.hasMore() || !Character.isLowerCase(line.nextChar()))
			return null;
		line.advance();
		char ch;
		while (line.hasMore() && ((ch = line.nextChar()) == '-' || Character.isLowerCase(ch) || Character.isDigit(ch)))
			line.advance();
		if (line.hasMore() && (Character.isUpperCase(line.nextChar()))) {
			errors.message(line, "template names may not include upper case characters");
			return null;
		}
		return errors.logParsingToken(new TemplateNameToken(pos.copySetEnd(line.at()), line.fromMark(mark)));
	}
	
	@Override
	public String toString() {
		return "TNT["+text+":" + location+"]";
	}

	@Override
	public InputPosition location() {
		return location;
	}

	@Override
	public String type() {
		return "templateName";
	}

	@Override
	public String text() {
		return text;
	}
}
