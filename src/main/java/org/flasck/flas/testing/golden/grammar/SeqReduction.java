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
	public final String reducesAs;
	private final List<SeqElement> matchers = new ArrayList<>();

	public SeqReduction(GrammarChooser chooser, Grammar g, SequenceDefinition d, String name, List<Boolean> os) {
		this.chooser = chooser;
		List<String> optionNames = new ArrayList<>();
		os = new ArrayList<>(os);
		for (int n=0;n<d.length();n++) {
			Definition x = d.nth(n);
			if (x instanceof IndentDefinition)
				continue; // not our concern
			SeqElement add = convert(g, x, os, optionNames);
			if (add != null)
				matchers.add(add);
		}
		String rn = d.reducesAs(optionNames);
		if (rn == null)
			this.reducesAs = name;
		else
			this.reducesAs = rn;
		chooser.addReduction(this.reducesAs, this);
	}

	private SeqElement convert(Grammar g, Definition x, List<Boolean> os, List<String> optionNames) {
		if (x instanceof TokenDefinition) {
			TokenDefinition td = (TokenDefinition)x;
			return new TokenElement(g, td);
		} else if (x instanceof ManyDefinition) {
			ManyDefinition md = (ManyDefinition) x;
			return new ManyElement(chooser, g, md);
		} else if (x instanceof RefDefinition) {
			RefDefinition rd = (RefDefinition) x;
			return new RefElement(chooser, rd);
		} else if (x instanceof OptionalDefinition) {
			OptionalDefinition od = (OptionalDefinition) x;
			boolean include = os.remove(0);
			if (!include)
				return null;
			optionNames.add(od.reducesAs());
			return convert(g, od.childRule(), os, optionNames);
		} else
			throw new NotImplementedException("converting " + x + " of " + x.getClass());
	}

	public boolean canBeKeyword(String keyword) {
		if (!canMatchOneToken())
			return false;
		if (matchers.get(0) instanceof TokenElement)
			return ((TokenElement)matchers.get(0)).canBeKeyword(keyword);
		// TODO Auto-generated method stub
		return false;
	}

	public boolean canMatchOneToken() {
		int cnt = 0;
		for (SeqElement m : matchers) {
			if (m instanceof TokenElement || m instanceof RefElement)
				cnt++;
		}
		return cnt <= 1;
	}

	public TrackProduction choose(String rule) {
		if (!canMatchOneToken())
			return null;
		for (SeqElement m : matchers) {
			if (m instanceof RefElement)
				return ((RefElement)m).choose(rule);
			else if (m instanceof ManyElement)
				return ((ManyElement)m).choose(rule);
		}
		return null;
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
