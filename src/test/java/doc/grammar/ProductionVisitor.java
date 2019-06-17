package doc.grammar;

import java.util.List;

import doc.grammar.SentenceProducer.UseNameForScoping;
import doc.grammar.TokenDefinition.Matcher;

public interface ProductionVisitor {

	boolean indent();
	void visit(Definition defn);
	void exdent();

	void choices(OrProduction prod, List<Definition> defns, List<Integer> probs, int maxProb);

	void zeroOrOne(Definition child);
	void zeroOrMore(Definition child, boolean withEOL);
	void oneOrMore(Definition child, boolean withEOL);

	void referTo(String child);

	void futurePattern(String amended, String pattern);
	void token(String token, String patternMatcher, UseNameForScoping scoping, List<Matcher> matchers);
}
