package org.flasck.flas.tokenizers;

public class VarNameToken {
	public static ValidIdentifierToken from(Tokenizable line) {
		line.skipWS();
		if (!line.hasMore())
			return null;

		char c = line.nextChar();
		if (Character.isLowerCase(c))
			return ValidIdentifierToken.from(line);
		else
			return null;
	}
}
