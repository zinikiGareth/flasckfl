package org.flasck.flas.tokenizers;

import org.flasck.flas.blockForm.InputPosition;

public class PackageNameToken {
	public final InputPosition location;
	public final String text;

	public PackageNameToken(ValidIdentifierToken tok) {
		location = tok.location;
		text = tok.text;
	}

	public PackageNameToken(InputPosition loc, String proto, int end) {
		location = loc;
		location.endAt(end);
		text = proto;
	}

	public static PackageNameToken from(Tokenizable line) {
		line.skipWS();
		if (!line.hasMore() || !Character.isJavaIdentifierStart(line.nextChar()))
			return null;
	
		InputPosition loc = line.realinfo();
		int mark = line.at();
		line.advance();
		while (line.hasMore() && (Character.isJavaIdentifierPart(line.nextChar()) || line.nextChar() == '.'))
			line.advance();
		return new PackageNameToken(loc, line.fromMark(mark), line.at());
	}
}
