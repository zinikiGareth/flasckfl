package org.flasck.flas.testing.golden;

import org.flasck.flas.grammar.Production;

public interface GrammarLevel {

	public boolean is(String rule);

	public Production choose(String rule);

}
