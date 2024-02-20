package org.flasck.flas.testing.golden.grammar;

import org.flasck.flas.grammar.Production;
import org.zinutils.exceptions.CantHappenException;
import org.zinutils.exceptions.NotImplementedException;

public class IndentAs implements TrackProduction {
	private final String reducesAs;
	private final TrackProduction rule;

	public IndentAs(String r, TrackProduction rule) {
		this.reducesAs = r;
		this.rule = rule;
	}

	@Override
	public void initWhenReady(Production prod) {
	}

	@Override
	public TrackProduction choose(String rule) {
		if (!rule.equals(reducesAs))
			throw new CantHappenException("can only handle " + reducesAs + " not " + rule);
		return this.rule;
	}

	@Override
	public String toString() {
		return "IndentAs[" + reducesAs + ":" + rule + "]";
	}
}
