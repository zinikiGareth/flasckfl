package org.flasck.flas.tokenizers;

import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.errors.ErrorReporter;
import org.flasck.flas.grammar.tracking.LoggableToken;

public class TypeNameToken implements LoggableToken {
	public final InputPosition location;
	public final String text;

	public TypeNameToken(ValidIdentifierToken tok) {
		location = tok.location;
		text = tok.text;
	}

	public TypeNameToken(InputPosition loc, String proto, int end) {
		location = loc;
		location.endAt(end);
		text = proto;
	}

	public static TypeNameToken unqualified(ErrorReporter errors, Tokenizable line) {
		line.skipWS(errors);
		if (!line.hasMore())
			return null;

		int mark = line.at();
		char c = line.nextChar();
		if (!Character.isUpperCase(c)) {
			// types must be upper case
			line.reset(mark);
			return null;
		}
		ValidIdentifierToken tok = ValidIdentifierToken.from(errors, line);
		if (tok.text.length() == 1 || tok.text.length() == 2 && (Character.isUpperCase(tok.text.charAt(1)) || Character.isDigit(tok.text.charAt(1)))) {
			// would be a poly var
			line.reset(mark);
			return null;
		}
		return errors.logParsingToken(new TypeNameToken(tok));
	}

	// qualified names are any number of (lowercase) package names followed by exactly one (uppercase) type name
	// we then stop; if there is another dot, it is probably a ctor method
	public static TypeNameToken qualified(ErrorReporter errors, Tokenizable line) {
		line.skipWS(errors);
		if (!line.hasMore() || !Character.isJavaIdentifierStart(line.nextChar()))
			return null;
	
		InputPosition loc = line.realinfo();
		int mark = line.at();
//		line.advance();
		boolean isNameStart = true;
		boolean haveName = false;
		while (line.hasMore()) {
			if (Character.isJavaIdentifierPart(line.nextChar())) {
				if (isNameStart) {
					if (Character.isUpperCase(line.nextChar()) ) {
						haveName = true;
					}
				}
				isNameStart = false;
			} else if (line.nextChar() == '.') {
				if (haveName) // we are done
					break;
				isNameStart = true;
			} else
				break;
			line.advance();
		}
		if (haveName)
			return errors.logParsingToken(new TypeNameToken(loc, line.fromMark(mark), line.at()));
		else {
			line.reset(mark); // we only saw lowercase things ...
			return null;
		}
	}
	
	@Override
	public InputPosition location() {
		return location;
	}

	@Override
	public String type() {
		return "TypeName";
	}

	@Override
	public String text() {
		return text;
	}

	@Override
	public String toString() {
		return "TNT[" + text + "]";
	}
}
