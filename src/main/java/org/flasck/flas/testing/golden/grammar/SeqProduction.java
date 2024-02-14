package org.flasck.flas.testing.golden.grammar;

import java.util.Map;
import java.util.TreeMap;

import org.flasck.flas.grammar.Grammar;
import org.flasck.flas.grammar.Production;
import org.flasck.flas.grammar.SequenceDefinition;
import org.zinutils.exceptions.CantHappenException;

public class SeqProduction implements TrackProduction {
	private final GrammarChooser chooser;
	private final Grammar grammar;
	private final String name;
	private final SequenceDefinition d;
	private final Map<String, SeqReduction> reduceAs = new TreeMap<>();

	public SeqProduction(GrammarChooser chooser, Grammar g, String name, SequenceDefinition d) {
		this.chooser = chooser;
		this.grammar = g;
		this.name = name;
		this.d = d;
	}

	public SeqProduction(GrammarChooser chooser, Grammar g, String reduction, SeqReduction reducer) {
		this.chooser = chooser;
		this.grammar = g;
		this.name = reduction;
		this.d = null;
		reduceAs.put(reduction, reducer);
	}

	@Override
	public void initWhenReady(Production prod) {
		if (d.reducesAs() != null) {
			SeqReduction that = new SeqReduction(chooser, grammar, d, d.reducesAs()); 
			reduceAs.put(d.reducesAs(), that);
		} else {
			SeqReduction that = new SeqReduction(chooser, grammar, d, name); 
			reduceAs.put(name, that);
		}
	}
	
	public String name() {
		return name;
	}

	@Override
	public TrackProduction choose(String rule) {
		if (name.equals(rule))
			return this;
		else if (reduceAs.containsKey(rule))
			return this;
		else
			return null;
	}
	
	public SeqReduction get(String name) {
		if (!reduceAs.containsKey(name))
			throw new CantHappenException("there is no reduction for " + name);
		return reduceAs.get(name);
	}

	@Override
	public String toString() {
		return name + "[]";
	}
}
