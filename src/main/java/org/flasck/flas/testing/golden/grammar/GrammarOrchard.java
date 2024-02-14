package org.flasck.flas.testing.golden.grammar;

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.flasck.flas.testing.golden.ParsedTokens;
import org.flasck.flas.testing.golden.ParsedTokens.GrammarStep;

public class GrammarOrchard implements Iterable<GrammarStep> {
	private final List<GrammarTree> trees = new ArrayList<>();

	public void add(GrammarTree tree) {
		trees.add(tree);
	}

	public int size() {
		return trees.size();
	}

	public void dump(PrintWriter pw) {
		for (GrammarTree t : trees) {
			t.dump(pw, "  ", false);
		}
	}

	@SuppressWarnings({"rawtypes", "unchecked"})
	@Override
	public Iterator<GrammarStep> iterator() {
		return (Iterator<GrammarStep>)(Iterator)trees.iterator();
	}
}
