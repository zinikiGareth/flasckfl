package org.flasck.flas.tokenizers;

import org.flasck.flas.errors.ErrorReporter;

public class VarNameToken {
	public static ValidIdentifierToken from(ErrorReporter errors, Tokenizable line) {
		line.skipWS(errors);
		if (!line.hasMore())
			return null;

		char c = line.nextChar();
		if (Character.isLowerCase(c))
			return ValidIdentifierToken.from(errors, line);
		else
			return null;
	}
}
