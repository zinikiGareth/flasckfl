package org.flasck.flas.testing.golden.grammar;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.flasck.flas.grammar.Definition;
import org.flasck.flas.grammar.Grammar;
import org.flasck.flas.grammar.IndentDefinition;
import org.flasck.flas.grammar.ManyDefinition;
import org.flasck.flas.grammar.OptionalDefinition;
import org.flasck.flas.grammar.RefDefinition;
import org.flasck.flas.grammar.SequenceDefinition;
import org.flasck.flas.grammar.TokenDefinition;
import org.zinutils.exceptions.NotImplementedException;

public class SeqReduction implements Iterable<SeqElement> {
	private final GrammarChooser chooser;
	private final String reducesAs;
	private final List<SeqElement> matchers = new ArrayList<>();

	public SeqReduction(GrammarChooser chooser, Grammar g, SequenceDefinition d, String reducesAs) {
		this.chooser = chooser;
		this.reducesAs = reducesAs;
		for (int n=0;n<d.length();n++) {
			Definition x = d.nth(n);
			if (x instanceof IndentDefinition)
				continue; // not our concern
			matchers.add(convert(g, x));
		}
		chooser.addReduction(reducesAs, this);
	}

	private SeqElement convert(Grammar g, Definition x) {
		if (x instanceof TokenDefinition) {
			TokenDefinition td = (TokenDefinition)x;
			return new TokenElement(g, td);
		} else if (x instanceof ManyDefinition) {
			ManyDefinition md = (ManyDefinition) x;
			return new ManyElement(g, md);
		} else if (x instanceof RefDefinition) {
			RefDefinition rd = (RefDefinition) x;
			return new RefElement(chooser, rd);
		} else if (x instanceof OptionalDefinition) {
			OptionalDefinition od = (OptionalDefinition) x;
			return new OptElement(g, od);
		} else
			throw new NotImplementedException("converting " + x + " of " + x.getClass());
	}

	@Override
	public Iterator<SeqElement> iterator() {
		return matchers.iterator();
	}

	@Override
	public String toString() {
		return reducesAs + ":Seq";
	}
}
