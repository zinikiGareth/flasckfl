package org.flasck.flas.tokenizers;

import org.flasck.flas.blockForm.InputPosition;

public class QualifiedTypeNameToken {
	public static TypeNameToken from(Tokenizable line) {
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
		return new TypeNameToken(loc, proto);
	}
}
