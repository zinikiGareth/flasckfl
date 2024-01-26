package org.flasck.flas.testing.golden;

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class GrammarOrchard implements Iterable<GrammarTree> {
	private List<GrammarTree> trees = new ArrayList<>();

	public void add(GrammarTree tree) {
		trees.add(tree);
	}

	public int size() {
		return trees.size();
	}

	public void dump(PrintWriter pw) {
		for (GrammarTree t : trees) {
			t.dump(pw, "  ");
		}
	}

	@Override
	public Iterator<GrammarTree> iterator() {
		return trees.iterator();
	}
}
