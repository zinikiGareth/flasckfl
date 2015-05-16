package org.flasck.flas.tokenizers;

public class TypeNameToken {
	public static String from(Tokenizable line) {
		line.skipWS();
		if (!line.hasMore())
			return null;

		char c = line.nextChar();
		if (Character.isUpperCase(c))
			return ValidIdentifierToken.from(line);
		else
			return null;
	}
}
