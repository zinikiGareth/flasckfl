package org.flasck.flas.testing.golden;

import java.util.ArrayList;
import java.util.List;

import org.flasck.flas.grammar.ActionDefinition;
import org.flasck.flas.grammar.Definition;
import org.flasck.flas.grammar.Grammar;
import org.flasck.flas.grammar.ManyDefinition;
import org.flasck.flas.grammar.OrProduction;
import org.flasck.flas.grammar.Production;
import org.flasck.flas.grammar.SequenceDefinition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.zinutils.exceptions.CantHappenException;
import org.zinutils.exceptions.NotImplementedException;

public class GrammarNavigator {
	public static final Logger logger = LoggerFactory.getLogger("GrammarChecker");
	private final Grammar grammar;
	private final List<GrammarLevel> stack = new ArrayList<>();

	public GrammarNavigator(Grammar grammar) {
		this.grammar = grammar;
	}

	public void push(Production rule) {
		if (rule instanceof OrProduction)
			stack.add(0, new OrChoice(grammar, (OrProduction)rule));
		else {
			Definition d = simplify(rule.defn);
			if (d instanceof ManyDefinition)
				stack.add(0, new ManyOf(grammar, (ManyDefinition)d));
			else
				throw new CantHappenException("can't handle " + rule.name + " " + d.getClass());
		}
	}

	/* The grammar can be cluttered with ActionDefinitions and that forces us into
	 * using <seq> operations to wrap them.  If we have something that isn't really a <seq>,
	 * return the thing that it really is.
	 */
	private Definition simplify(Definition defn) {
		if (defn instanceof SequenceDefinition) {
			Definition ret = null;
			SequenceDefinition seq = (SequenceDefinition) defn;
			for (int i=0;i<seq.length();i++) {
				Definition x = seq.nth(i);
				if (x instanceof ActionDefinition)
					continue;
				if (ret == null)
					ret = x; // the first thing is the only one that can be simple ...
				else
					return defn; // it has at least two real things in it
			}
			return ret;
		} else
			return defn;
	}

	@Override
	public String toString() {
		return "GrammarNavigator: " + stack;
	}

	public boolean isAtEnd() {
		return true;
	}

	public Production findChooseableRule(String rule) {
		GrammarLevel top = stack.get(0);
		if (top.is(rule))
			throw new NotImplementedException("we are already there");
		Production r = top.choose(rule);
		if (r != null)
			return r;
		return null;
	}
}
