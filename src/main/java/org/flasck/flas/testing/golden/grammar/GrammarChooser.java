package org.flasck.flas.testing.golden.grammar;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.flasck.flas.doc.grammar.GenerateGrammarDoc;
import org.flasck.flas.grammar.ActionDefinition;
import org.flasck.flas.grammar.Definition;
import org.flasck.flas.grammar.Grammar;
import org.flasck.flas.grammar.ManyDefinition;
import org.flasck.flas.grammar.OrProduction;
import org.flasck.flas.grammar.Production;
import org.flasck.flas.grammar.RefDefinition;
import org.flasck.flas.grammar.SequenceDefinition;
import org.flasck.flas.grammar.TokenDefinition;
import org.flasck.flas.testing.golden.ParsedTokens.GrammarStep;
import org.zinutils.exceptions.CantHappenException;
import org.zinutils.exceptions.NotImplementedException;

public class GrammarChooser {
	private final Grammar orig;
	private final Map<String, TrackProduction> grammar = new TreeMap<>();
	private final Map<String, SeqProduction> reductions = new TreeMap<>();

	public GrammarChooser(Grammar grammar) {
		orig = grammar;
		GenerateGrammarDoc.checkTokens(grammar);
		GenerateGrammarDoc.checkProductions(grammar);
		populateGrammar(grammar);
	}

	private void populateGrammar(Grammar g) {
		// Create a placeholder for every rule
		for (Production p : g.productions()) {
			grammar.put(p.name, makeThing(p));
		}
		
		// allow it to initialise once all the rules are loaded
		for (Production p : g.productions()) {
			TrackProduction ret = grammar.get(p.name);
			if (ret != null)
				ret.initWhenReady(p);
		}
	}
	
	private TrackProduction makeThing(Production rule) {
		if (rule instanceof OrProduction)
			return new OrChoice(orig, this, (OrProduction)rule);
		else {
			return convert(rule.name, rule.defn);
		}
	}
	

	public TrackProduction convert(String name, Definition defn) {
		Definition d = simplify(defn);
		if (d instanceof ManyDefinition) {
			return new ManyProduction(this, name, (ManyDefinition)d);
		} else if (d instanceof RefDefinition) {
			return new RefProduction(this, name, (RefDefinition)d);
		} else if (d instanceof SequenceDefinition) {
			return new SeqProduction(this, orig, name, (SequenceDefinition)d);
		} else if (d instanceof TokenDefinition) {
			return new TokenProduction(orig, name, (TokenDefinition)d);
		} else {
//			System.out.println("can't handle " + rule.name + " " + d.getClass());
			throw new CantHappenException("can't handle " + name + " " + d.getClass());
		}
	}

	/* The grammar can be cluttered with ActionDefinitions and that forces us into
	 * using <seq> operations to wrap them.  If we have something that isn't really a <seq>,
	 * return the thing that it really is.
	 * 
	 * Otherwise, rebuild the sequence without the clutter
	 */
	private Definition simplify(Definition defn) {
		if (defn instanceof SequenceDefinition) {
			List<Definition> ret = new ArrayList<>();
			SequenceDefinition seq = (SequenceDefinition) defn;
			for (int i=0;i<seq.length();i++) {
				Definition x = seq.nth(i);
				if (x instanceof ActionDefinition)
					continue;
				ret.add(x);
			}
			if (!seq.hasExplicitReduceAs() && ret.size() == 1)
				return ret.get(0);
			else
				return seq.cloneWith(ret);
		} else
			return defn;
	}

	public void addReduction(String reduction, SeqReduction reducer) {
		if (reductions.containsKey(reduction)) {
			return;
		}
		reductions.put(reduction, new SeqProduction(this, orig, reduction, reducer));
	}
	
	public GrammarNavigator newNavigator() {
		return new GrammarNavigator(this);
	}

	public boolean hasRule(String want) {
		return grammar.containsKey(want);
	}

	public TrackProduction rule(String rule) {
		if (!grammar.containsKey(rule))
			throw new CantHappenException("there is no rule " + rule);
		return grammar.get(rule);
	}

	public SeqProduction findReduction(GrammarTree tree) {
		String rule = tree.reducedToRule();
		SeqProduction ret = reductions.get(rule);
		if (ret != null)
			return ret;
		TrackProduction prod = grammar.get(rule);
		if (prod != null) {
			if (prod instanceof SeqProduction) {
				return (SeqProduction) prod;
			} else if (prod instanceof OrChoice) {
				// For this to work, the tree must be a singleton with another rule inside, I think
				if (!tree.isSingleton()) {
					throw new CantHappenException("the tree is not a singleton");
				}
				GrammarStep s = tree.members().next();
				if (!(s instanceof GrammarTree))
					throw new CantHappenException("the singleton is not a tree");
				TrackProduction choice = ((OrChoice)prod).choose(((GrammarTree)s).reducedToRule());
				if (choice == null)
					return null;
				if (choice instanceof SeqProduction)
					return (SeqProduction) choice;
				else
					throw new NotImplementedException("have found " + choice + ": " + choice.getClass());
			} else
				throw new NotImplementedException("the prod is " + prod + ": " + prod.getClass());
		}
		throw new CantHappenException("there is nothing matching " + tree);
	}
}
