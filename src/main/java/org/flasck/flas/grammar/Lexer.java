package org.flasck.flas.grammar;

public class Lexer implements Comparable<Lexer> {
	public final String token;
	public final String pattern;
	public final String desc;

	public Lexer(String token, String pattern, String desc) {
		this.token = token;
		this.pattern = pattern;
		this.desc = desc;
	}

	@Override
	public int compareTo(Lexer o) {
		return token.compareTo(o.token);
	}
}
