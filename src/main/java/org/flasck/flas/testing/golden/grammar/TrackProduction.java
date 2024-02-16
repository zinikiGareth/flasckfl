package org.flasck.flas.testing.golden.grammar;

import org.flasck.flas.grammar.Production;

public interface TrackProduction {

	public void initWhenReady(Production prod);
	
//	public boolean is(String rule);

	public TrackProduction choose(String rule);
	
	public default boolean canBeKeyword(String keyword) { return false; }
}
