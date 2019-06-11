package doc.grammar;

import java.util.List;

public interface ProductionVisitor {

	void choices(OrProduction prod, List<Definition> defns, List<Integer> probs, int maxProb);

	void zeroOrOne(Definition child);
	void zeroOrMore(Definition child);
	void oneOrMore(Definition child);

	void referTo(String child);

	void token(String token);

	boolean indent();

	void exdent();
}
