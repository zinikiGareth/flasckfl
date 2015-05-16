package org.flasck.flas.tokenizers;

public class TypeExprToken {
	public static final int NAME = 1;

	public static final int ORB = 10;
	public static final int CRB = 11;
	public static final int OSB = 12;
	public static final int CSB = 13;
	public static final int COMMA = 14;
	
	public final int type;
	public final String text;

	public TypeExprToken(int type, String text) {
		this.type = type;
		this.text = text;
		System.out.println(type +":"+text);
	}

	public static TypeExprToken from(Tokenizable line) {
		line.skipWS();
		if (!line.hasMore())
			return null;
		char c = line.nextChar();
		int pos;
		if (Character.isUpperCase(c))
			return new TypeExprToken(NAME, ValidIdentifierToken.from(line));
		else if ((pos = "()[],".indexOf(c)) != -1) {
			line.advance();
			return new TypeExprToken(10+pos, null);
		}
		else
			return null;
	}

	@Override
	public String toString() {
		return "PT[" + type + ":" + text + "]";
	}
}
