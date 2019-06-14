package doc.grammar;

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.apache.commons.lang3.StringEscapeUtils;

public class TokenDefinition extends Definition {
	public class Matcher {
		public final String amendedName;
		public final String pattern;

		public Matcher(String amendedName, String pattern) {
			this.amendedName = amendedName;
			this.pattern = pattern;
		}
	}

	private final String token;
	private final String patternMatcher;
	private final List<Matcher> matchers = new ArrayList<>();

	public TokenDefinition(String token, String patternMatcher) {
		this.token = token;
		this.patternMatcher = patternMatcher;
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
		productionVisitor.token(token, patternMatcher, matchers);
	}

	public void addMatcher(String amendedName, String pattern) {
		matchers.add(new Matcher(amendedName, pattern));
	}
}
