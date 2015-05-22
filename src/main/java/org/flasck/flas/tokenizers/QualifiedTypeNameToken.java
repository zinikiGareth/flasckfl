package org.flasck.flas.tokenizers;

public class QualifiedTypeNameToken {
	public static String from(Tokenizable line) {
		line.skipWS();
		if (!line.hasMore() || !Character.isJavaIdentifierStart(line.nextChar()))
			return null;

		int mark = line.at();
		line.advance();
		while (line.hasMore() && (Character.isJavaIdentifierPart(line.nextChar()) || line.nextChar() == '.'))
			line.advance();
		return line.fromMark(mark);
	}
}
