package org.flasck.flas.grammar;

import java.util.List;

import org.flasck.flas.grammar.SentenceProducer.UseNameForScoping;
import org.flasck.flas.grammar.TokenDefinition.Matcher;

public interface ProductionVisitor {

	boolean indent(boolean force);
	void visit(Definition defn);
	void exdent();

	void choices(OrProduction prod, Object cxt, List<Definition> defns, List<Integer> probs, int maxProb, boolean repeatVarName);
	boolean complete(OrProduction prod, Object cxt, List<Definition> choices);

	void zeroOrOne(Definition child);
	int zeroOrMore(Definition child, boolean withEOL);
	int oneOrMore(Definition child, boolean withEOL);
	void exactly(int cnt, Definition child, boolean withEOL);

	void referTo(String child, boolean resetToken);
	OrProduction isOr(String child);

	void futurePattern(String amended, String pattern);
	void token(String token, String patternMatcher, UseNameForScoping scoping, List<Matcher> matchers, boolean repeatLast, boolean saveLast, String generator, boolean space);
	void generateEOL();
	void nestName(int offset);
	void pushPart(String prefix, String names, boolean appendFileName);

	void setDictEntry(String var, String val);
	String getDictValue(String var);
	String getTopDictValue(String var);
	void clearDictEntry(String var);
	void condNotEqual(String var, String ne, Definition inner);
	void condNotSet(String var, Definition inner);
	void pushCaseNumber();
}
