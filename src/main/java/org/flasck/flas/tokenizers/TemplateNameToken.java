package org.flasck.flas.tokenizers;

import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.errors.ErrorReporter;

public class TemplateNameToken {
	public final InputPosition location;
	public final String text;

	public TemplateNameToken(InputPosition location, String text) {
		this.location = location;
		this.text = text;
	}

	public static TemplateNameToken from(ErrorReporter errors, Tokenizable line) {
		line.skipWS();
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
		return new TemplateNameToken(pos.copySetEnd(line.at()), line.fromMark(mark));
	}
	
	@Override
	public String toString() {
		return "TNT["+text+":" + location+"]";
	}
}
