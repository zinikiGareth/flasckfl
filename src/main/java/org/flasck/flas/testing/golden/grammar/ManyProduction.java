package org.flasck.flas.testing.golden.grammar;

import org.flasck.flas.grammar.Definition;
import org.flasck.flas.grammar.ManyDefinition;
import org.flasck.flas.grammar.Production;
import org.flasck.flas.grammar.RefDefinition;
import org.zinutils.exceptions.HaventConsideredThisException;

// THIS IS ONLY FOR PRODUCTIONS WITH A SINGLE MANY RULE
public class ManyProduction implements TrackProduction {
	private final GrammarChooser nav;
	private final String name;
	private final ManyDefinition d;
	private TrackProduction repeater;

	public ManyProduction(GrammarChooser grammarChooser, String name, ManyDefinition d) {
		this.nav = grammarChooser;
		this.name = name;
		this.d = d;
	}

	@Override
	public void initWhenReady(Production prod) {
		Definition c = d.repeats();
		if (c instanceof RefDefinition) {
			RefDefinition rd = (RefDefinition) c;
			repeater = nav.rule(rd.ruleName());
		} else
			throw new HaventConsideredThisException("Many with a " + d.getClass() + " child");
	}

//	@Override
//	public boolean is(String rule) {
//		return false;
//	}

	@Override
	public TrackProduction choose(String rule) {
		if (name.equals(rule))
			return this;
		if (repeater == null)
			return null;
		return repeater.choose(rule);
	}

	@Override
	public String toString() {
		return name + ":*"+d;
	}
}
