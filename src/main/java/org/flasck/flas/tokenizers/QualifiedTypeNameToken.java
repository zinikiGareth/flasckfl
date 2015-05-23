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
		String proto = line.fromMark(mark);
		int pos = proto.lastIndexOf('.')+1;
		if (!Character.isUpperCase(proto.charAt(pos)))
			return null; // doesn't qualify
		return proto;
	}
}
