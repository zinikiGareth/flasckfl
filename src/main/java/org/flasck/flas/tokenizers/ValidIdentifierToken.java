package org.flasck.flas.tokenizers;

public class ValidIdentifierToken {

	public static String from(Tokenizable line) {
		line.skipWS();
		int mark = line.at();
		if (!line.hasMore() || !Character.isJavaIdentifierStart(line.nextChar()))
			return null;
		line.advance();
		while (line.hasMore() && Character.isJavaIdentifierPart(line.nextChar()))
			line.advance();
		return line.fromMark(mark);
	}

}
