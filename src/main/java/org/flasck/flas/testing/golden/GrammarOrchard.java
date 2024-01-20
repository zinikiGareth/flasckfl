package org.flasck.flas.testing.golden;

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

public class GrammarOrchard {
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
}
