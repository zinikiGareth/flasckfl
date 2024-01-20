package org.flasck.flas.testing.golden;

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.testing.golden.ParsedTokens.GrammarStep;
import org.flasck.flas.testing.golden.ParsedTokens.GrammarToken;
import org.flasck.flas.testing.golden.ParsedTokens.ReductionRule;

public class GrammarTree implements GrammarStep {
	private ReductionRule reducedTo;
	private List<GrammarStep> members = new ArrayList<>();

	public GrammarTree(ReductionRule rr) {
		this.reducedTo = rr;
	}

	public void dump(PrintWriter pw, String ind) {
		pw.print(ind);
		pw.print(reducedTo.ruleName());
		pw.println();
		String nested = ind + "  ";
		for (GrammarStep s : members) {
			if (s instanceof GrammarTree) {
				((GrammarTree)s).dump(pw, nested);
			} else if (s instanceof GrammarToken) {
				pw.print(nested);
				pw.print(s);
				pw.println();
			}
		}
	}

	public void push(GrammarStep si) {
		members.add(0, si);
	}

	@Override
	public InputPosition location() {
		return reducedTo.location();
	}
	
	@Override
	public String toString() {
		return location() + ": " + reducedTo;
	}
}
