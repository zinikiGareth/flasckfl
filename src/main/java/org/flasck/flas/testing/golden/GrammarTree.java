package org.flasck.flas.testing.golden;

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.testing.golden.ParsedTokens.GrammarStep;
import org.flasck.flas.testing.golden.ParsedTokens.GrammarToken;
import org.flasck.flas.testing.golden.ParsedTokens.ReductionRule;
import org.zinutils.exceptions.CantHappenException;

public class GrammarTree implements GrammarStep {
	private final ReductionRule reducedTo;
	private final List<GrammarStep> members = new ArrayList<>();
	private final List<GrammarTree> indents;

	public GrammarTree(ReductionRule rr) {
		this.reducedTo = rr;
		this.indents = new ArrayList<>();
	}

	public GrammarTree(String topRule, List<GrammarTree> ret) {
		this.reducedTo = new ReductionRule(topRule);
		this.indents = ret;
	}

	public void dump(PrintWriter pw, String ind, boolean isIndented) {
		pw.print(ind);
		if (isIndented)
			pw.print(">> ");
		pw.print(reducedTo.ruleName());
		pw.println();
		String nested = ind + "   ";
		for (GrammarStep s : members) {
			if (s instanceof GrammarTree) {
				((GrammarTree)s).dump(pw, nested, false);
			} else if (s instanceof GrammarToken) {
				GrammarToken tok = (GrammarToken) s;
				pw.print(nested);
				pw.print(tok.type + ": _" + tok.text + "_");
				pw.println();
			}
		}
		for (GrammarTree t : indents) {
			t.dump(pw, nested, true);
		}
	}

	public void push(GrammarStep si) {
		System.out.println("pushing " + si);
		System.out.println("pushing " + si.location() + " with " + location());
		if (isIndented(si.location(), location())) {
			if (!(si instanceof GrammarTree))
				throw new CantHappenException("can't push " + si + " because it is directly indented not reduced");
			indents.add(0, (GrammarTree) si);
		} else
			members.add(0, si);
	}

	private boolean isIndented(InputPosition item, InputPosition relativeTo) {
		if (item.lineNo == relativeTo.lineNo)
			return false;
		if (item.lineNo < relativeTo.lineNo)
			throw new CantHappenException("cannot come before the relative line");
		if (item.indent.tabs < relativeTo.indent.tabs)
			throw new CantHappenException("cannot be indented less than relative line");
		return true;
	}

	@Override
	public InputPosition location() {
		return reducedTo.location();
	}
	
	public String reducedToRule() {
		return reducedTo.ruleName();
	}
	
	@Override
	public String toString() {
		return location() + ": " + reducedTo;
	}

	public Iterator<GrammarStep> members() {
		return members.iterator();
	}
}
