package doc.grammar;

import java.util.List;

public interface ProductionVisitor {

	void choices(List<Definition> asList);

	void zeroOrOne(Definition child);
	void zeroOrMore(Definition child);

	void referTo(String child);

	void token(String token);

	void indent();

	void exdent();


}
