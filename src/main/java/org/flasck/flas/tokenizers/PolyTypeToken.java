package org.flasck.flas.tokenizers;

import org.flasck.flas.blockForm.InputPosition;

public class PolyTypeToken {
	public final InputPosition location;
	public final String text;

	public PolyTypeToken(ValidIdentifierToken tok) {
		location = tok.location;
		text = tok.text;
	}

	public static PolyTypeToken from(Tokenizable line) {
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
		if (tok.text.length() > 2 || (tok.text.length() == 2 && !Character.isUpperCase(tok.text.charAt(1)) && !Character.isDigit(tok.text.charAt(1)))) {
			// would be a real type
			line.reset(mark);
			return null;
		}
		return new PolyTypeToken(tok);
	}
}
