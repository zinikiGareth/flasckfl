package doc.grammar;

import java.io.PrintWriter;
import java.util.Set;

import org.apache.commons.lang3.StringEscapeUtils;

public class TokenDefinition extends Definition {
	private final String token;
	private final String patternMatcher;

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
		productionVisitor.token(token, patternMatcher);
	}

}
