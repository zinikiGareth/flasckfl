package org.flasck.flas.testing.golden.grammar;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.flasck.flas.grammar.Definition;
import org.flasck.flas.grammar.Grammar;
import org.flasck.flas.grammar.IndentDefinition;
import org.flasck.flas.grammar.OptionalDefinition;
import org.flasck.flas.grammar.Production;
import org.flasck.flas.grammar.RefDefinition;
import org.flasck.flas.grammar.SequenceDefinition;
import org.zinutils.collections.CollectionUtils;
import org.zinutils.exceptions.CantHappenException;

public class SeqProduction implements TrackProduction {
	private final GrammarChooser chooser;
	private final Grammar grammar;
	private final String name;
	private final SequenceDefinition d;
	private final Map<String, SeqReduction> reduceAs = new TreeMap<>();
	private TrackProduction indent;

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
		List<List<Boolean>> opts = figureOptions();
		for (List<Boolean> os : opts) {
			SeqReduction that = new SeqReduction(chooser, grammar, d, name, os); 
			reduceAs.put(that.reducesAs, that);
		}
		indent = figureIndent();
	}
	
	private List<List<Boolean>> figureOptions() {
		List<List<Boolean>> ret = new ArrayList<>();
		int cnt = 0;
		for (int i=0;i<d.length();i++) {
			if (d.nth(i) instanceof OptionalDefinition)
				cnt++;
		}

		// [[False], [True]]
		// [[False, False], [False,True], [True,False], [True, True]]
		for (int j=0;j<(1<<cnt);j++) {
			List<Boolean> it = new ArrayList<>();
			ret.add(it);
			for (int k=0;k<cnt;k++) {
				it.add(0, (j & (1<<k)) != 0);
			}
		}
//		System.out.println(name + " " + d.reducesAs() + " " + ret);
		return ret;
	}

	private TrackProduction figureIndent() {
		Definition last = d.nth(d.length()-1);
		if (!(last instanceof IndentDefinition)) {
			if (!d.borrowFinalIndent())
				return null;
			RefDefinition rd = (RefDefinition) last;
			String refersTo = rd.ruleName();
			Production other = grammar.findRule(refersTo);
			SequenceDefinition od = (SequenceDefinition) other.defn;
			last = od.nth(od.length()-1);
		}
		IndentDefinition id = (IndentDefinition)last;
		String r = id.reducesTo();
		Definition rd = id.indented();
		TrackProduction rule = null;
		if (rd instanceof RefDefinition)
			rule = chooser.rule(((RefDefinition)rd).ruleName());
		else
			throw new CantHappenException("rd is " + rd.getClass());
		if (r == null)
			return rule;
		else
			return new IndentAs(r, rule);
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
		else if (reduceAs.size() == 1)
			return CollectionUtils.any(reduceAs.values()).choose(rule);
		else
			return null;
	}
	
	@Override
	public boolean canBeKeyword(String keyword) {
		for (SeqReduction e : reduceAs.values()) {
			if (e.canBeKeyword(keyword))
				return true;
		}
		return false;
	}
	
	public SeqReduction get(String name) {
		if (!reduceAs.containsKey(name))
			throw new CantHappenException("there is no reduction for " + name);
		return reduceAs.get(name);
	}
	
	public TrackProduction indented() {
		return indent;
	}

	@Override
	public String toString() {
		return name + "[]";
	}
}
