package doc.grammar;

import java.util.List;

public interface ProductionVisitor {

	void choices(OrProduction prod, List<Definition> defns, List<Integer> probs, int maxProb);

	void zeroOrOne(Definition child);
	void zeroOrMore(Definition child, boolean withEOL);
	void oneOrMore(Definition child, boolean withEOL);

	void referTo(String child);

	void token(String token, String patternMatcher);

	boolean indent();

	void exdent();

	void visit(Definition defn);

}
