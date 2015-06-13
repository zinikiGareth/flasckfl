package org.flasck.flas.tokenizers;

import org.flasck.flas.blockForm.InputPosition;

public class TypeNameToken {
	public final InputPosition location;
	public final String text;

	public TypeNameToken(ValidIdentifierToken tok) {
		location = tok.location;
		text = tok.text;
	}

	public TypeNameToken(InputPosition loc, String proto) {
		location = loc;
		text = proto;
	}

	public static TypeNameToken from(Tokenizable line) {
		line.skipWS();
		if (!line.hasMore())
			return null;

		char c = line.nextChar();
		if (Character.isUpperCase(c))
			return new TypeNameToken(ValidIdentifierToken.from(line));
		else
			return null;
	}
}
