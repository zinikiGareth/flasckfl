package org.flasck.flas.tokenizers;

import org.flasck.flas.blockForm.InputPosition;

public class TypeNameToken {
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

	public static TypeNameToken unqualified(Tokenizable line) {
		line.skipWS();
		if (!line.hasMore())
			return null;

		int mark = line.at();
		char c = line.nextChar();
		if (!Character.isUpperCase(c)) {
			// types must be upper case
			line.reset(mark);
			return null;
		}
		ValidIdentifierToken tok = ValidIdentifierToken.from(line);
		if (tok.text.length() == 1 || tok.text.length() == 2 && (Character.isUpperCase(tok.text.charAt(1)) || Character.isDigit(tok.text.charAt(1)))) {
			// would be a poly var
			line.reset(mark);
			return null;
		}
		return new TypeNameToken(tok);
	}

	public static TypeNameToken qualified(Tokenizable line) {
		line.skipWS();
		if (!line.hasMore() || !Character.isJavaIdentifierStart(line.nextChar()))
			return null;
	
		InputPosition loc = line.realinfo();
		int mark = line.at();
		line.advance();
		while (line.hasMore() && (Character.isJavaIdentifierPart(line.nextChar()) || line.nextChar() == '.'))
			line.advance();
		String proto = line.fromMark(mark);
		int pos = proto.lastIndexOf('.')+1;
		if (!Character.isUpperCase(proto.charAt(pos))) {
			line.reset(mark);
			return null; // doesn't qualify
		}
		return new TypeNameToken(loc, proto, line.at());
	}
}
