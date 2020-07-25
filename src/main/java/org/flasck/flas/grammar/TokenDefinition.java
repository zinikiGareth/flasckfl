package org.flasck.flas.grammar;

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.apache.commons.lang3.StringEscapeUtils;
import org.flasck.flas.grammar.SentenceProducer.UseNameForScoping;

public class TokenDefinition extends Definition {
	public class Matcher {
		public final String amendedName;
		public final String pattern;
		public final UseNameForScoping scoper;

		public Matcher(String amendedName, String pattern, UseNameForScoping scoper) {
			this.amendedName = amendedName;
			this.pattern = pattern;
			this.scoper = scoper;
		}
	}

	private final String token;
	private final String patternMatcher;
	private final UseNameForScoping scoping;
	private final List<Matcher> matchers = new ArrayList<>();

	public TokenDefinition(String token, String patternMatcher, UseNameForScoping scoping) {
		this.token = token;
		this.patternMatcher = patternMatcher;
		this.scoping = scoping;
	}

	@Override
	public void showGrammarFor(PrintWriter str) {
		str.append("<span class='production-token'>" + StringEscapeUtils.escapeHtml4(token) + "</span>");
	}

	@Override
	public void collectReferences(Set<String> ret) {
	}

	@Override
	public void collectTokens(Set<String> ret) {
		ret.add(token);
	}

	@Override
	public void visit(ProductionVisitor productionVisitor) {
		productionVisitor.token(token, patternMatcher, scoping, matchers);
	}

	public void addMatcher(String amendedName, String pattern, UseNameForScoping scoper) {
		matchers.add(new Matcher(amendedName, pattern, scoper));
	}
	
	@Override
	public String toString() {
		return "Token[" + token + "]";
	}
}
