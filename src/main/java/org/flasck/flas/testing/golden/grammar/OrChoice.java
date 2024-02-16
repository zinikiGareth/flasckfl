package org.flasck.flas.testing.golden.grammar;

import java.util.ArrayList;
import java.util.List;

import org.flasck.flas.grammar.Definition;
import org.flasck.flas.grammar.Grammar;
import org.flasck.flas.grammar.OrProduction;
import org.flasck.flas.grammar.Production;
import org.flasck.flas.grammar.RefDefinition;
import org.flasck.flas.grammar.SequenceDefinition;
import org.flasck.flas.grammar.TokenDefinition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.zinutils.exceptions.CantHappenException;
import org.zinutils.exceptions.HaventConsideredThisException;

public class OrChoice implements TrackProduction {
	public static final Logger logger = LoggerFactory.getLogger("GrammarChecker");
	private final Grammar grammar;
	private final GrammarChooser chooser;
	private final String prod;
	private final List<TrackProduction> choices = new ArrayList<>();

	public OrChoice(Grammar grammar, GrammarChooser grammarChooser, OrProduction rule) {
		this.grammar = grammar;
		this.chooser = grammarChooser;
		this.prod = rule.name;
	}
	
	@Override
	public void initWhenReady(Production prod) {
		int k = 0;
		for (Definition d : ((OrProduction)prod).allOptions()) {
//			logger.info("converting " + d);
			if (d instanceof RefDefinition) {
				choices.add(chooser.rule(((RefDefinition)d).ruleName()));
			} else if (d instanceof TokenDefinition) {
				String ruleName = "tok_" + k;
				k++;
				choices.add(new TokenProduction(grammar, ruleName, (TokenDefinition) d));
			} else if (d instanceof SequenceDefinition) {
				SequenceDefinition sd = (SequenceDefinition)d;
				String ruleName;
				if (sd.reducesAs() != null) {
					ruleName = sd.reducesAs();
				} else {
					ruleName = "seq_" + k;
					k++;
				}
				TrackProduction cnv = chooser.convert(ruleName, sd);
				cnv.initWhenReady(prod);
				choices.add(cnv);
			} else {
				throw new HaventConsideredThisException("or with " + d.getClass());
			}
		}
	}

//	@Override
//	public boolean is(String rule) {
//		return this.prod.equals(rule);
//	}

	@Override
	public TrackProduction choose(String rule) {
		logger.info("or rule " + prod + " looking for " + rule);
		if (prod.equals(rule))
			return this;
		for (TrackProduction d : choices) {
			logger.info("considering " + d);
			TrackProduction k = d.choose(rule);
			if (k != null)
				return k;
		}
		logger.info("none found");
		return null;
	}

	public SeqReduction getSequence(String rule) {
		TrackProduction sp = choose(rule);
		if (sp == null)
			return null;
		else if (sp instanceof SeqProduction)
			return ((SeqProduction)sp).get(rule);
		else
			throw new CantHappenException(rule + " was not a sequence");
	}

	@Override
	public String toString() {
		return prod +":||";
	}
}
