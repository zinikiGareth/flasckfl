package doc.grammar;

import java.io.PrintWriter;
import java.util.Set;

public abstract class Definition {
	public abstract void showGrammarFor(PrintWriter str);

	public abstract void collectReferences(Set<String> ret);

	public abstract void collectTokens(Set<String> ret);
}
