package org.flasck.flas.lifting;

import java.util.Collection;
import java.util.List;
import java.util.Set;

import org.flasck.flas.commonBase.Pattern;
import org.flasck.flas.parsedForm.HandlerImplements;
import org.flasck.flas.parsedForm.HandlerLambda;
import org.flasck.flas.parsedForm.LogicHolder;
import org.flasck.flas.parsedForm.UnresolvedVar;
import org.flasck.flas.patterns.HSIOptions;

public interface NestedVarReader {
	int size();
	Collection<HSIOptions> all();
	List<UnresolvedVar> vars();
	List<Pattern> patterns();
	boolean containsReferencesNotIn(Set<LogicHolder> processedFns);
	Set<LogicHolder> references();
	Set<HandlerImplements> referencesHI();
	Set<LogicHolder> referencesHIMethods();
	boolean enhanceWith(LogicHolder fn, NestedVarReader nestedVars);
	boolean dependsOn(LogicHolder f);
	void clearPatterns();
	void bindLambda(int which, HandlerLambda hl);
}
