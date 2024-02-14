package org.flasck.flas.testing.golden.grammar;

import java.util.ArrayList;
import java.util.List;

import org.flasck.flas.grammar.Production;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.zinutils.exceptions.CantHappenException;
import org.zinutils.exceptions.NotImplementedException;

public class GrammarNavigator {
	public static final Logger logger = LoggerFactory.getLogger("GrammarChecker");
	private final GrammarChooser grammar;
	private final List<TrackProduction> stack = new ArrayList<>();

	public GrammarNavigator(GrammarChooser grammar) {
		this.grammar = grammar;
		this.push(grammar.rule("file"));
	}

	public void push(TrackProduction rule) {
		stack.add(0, rule);
	}
	
	public void pop() {
		stack.remove(0);
	}

	public boolean isAtEnd() {
		return true;
	}

	public TrackProduction findChooseableRule(String rule) {
		TrackProduction top = stack.get(0);
//		if (top.is(rule))
//			throw new NotImplementedException("we are already there");
		TrackProduction r = top.choose(rule);
		if (r != null)
			return r;
		return null;
	}

	public SeqReduction sequence(String rule) {
		TrackProduction top = stack.get(0);
		if (top instanceof SeqProduction)
			return ((SeqProduction) top).get(rule);
		else if (top instanceof OrChoice) {
			OrChoice c = (OrChoice) top;
			System.out.println("c = " + c);
			return c.getSequence(rule);
		} else
			throw new CantHappenException("the top eleement was not a SeqReduction but " + top.getClass());
	}

	@Override
	public String toString() {
		return "GrammarNavigator: " + stack;
	}
}
