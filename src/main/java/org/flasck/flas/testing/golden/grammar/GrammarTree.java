package org.flasck.flas.testing.golden.grammar;

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.testing.golden.ParsedTokens;
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
				pw.print(tok.type + ": _" + tok.text + "_" + " [" + tok.location().lineNo + "." + tok.location().off + "]");
				pw.println();
			}
		}
		for (GrammarTree t : indents) {
			t.dump(pw, nested, true);
		}
	}

	public void push(GrammarStep si) {
//		System.out.println("pushing " + si);
//		System.out.println("pushing " + si.location() + " with " + location());
		if (isIndented(si.location(), location()) && !ftt(si)) {
			if (!(si instanceof GrammarTree))
				throw new CantHappenException("can't push " + si + " because it is directly indented not reduced");
			indents.add(0, (GrammarTree) si);
		} else
			members.add(0, si);
	}

	private boolean ftt(GrammarStep si) {
		if (!(si instanceof GrammarToken))
			return false;
		GrammarToken t = (GrammarToken) si;
		return t.type.equals("FREETEXT");
	}

	private boolean isIndented(InputPosition item, InputPosition relativeTo) {
		if (item.lineNo == relativeTo.lineNo)
			return false;
		if (item.lineNo < relativeTo.lineNo)
			throw new CantHappenException("cannot come before the relative line");
		if (item.indent.tabs < relativeTo.indent.tabs)
			throw new CantHappenException("cannot be indented less than relative line");
		if (item.indent.spaces > 0)
			return false;
		return true;
	}

	@Override
	public InputPosition location() {
		return reducedTo.location();
	}
	
	public String reducedToRule() {
		return reducedTo.ruleName();
	}
	
	public boolean hasMembers() {
		return !members.isEmpty();
	}

	public boolean isSingleton() {
		return members.size() == 1 && members.get(0) instanceof GrammarTree;
	}
	
	public boolean isTerminal() {
		return members.size() == 1 && members.get(0) instanceof GrammarToken;
	}

	public GrammarTree singleton() {
		if (members.size() != 1)
			throw new CantHappenException("singleton should have one member");
		if (!(members.get(0) instanceof GrammarTree))
			throw new CantHappenException("singleton member should be a tree");
		return (GrammarTree) members.get(0);
	}

	public GrammarToken terminal() {
		if (members.size() != 1)
			throw new CantHappenException("terminal should have one member");
		if (!(members.get(0) instanceof GrammarToken))
			throw new CantHappenException("terminal member should be a token, not " + members.get(0));
		return (GrammarToken) members.get(0);
	}

	public Iterator<GrammarStep> members() {
		return members.iterator();
	}
	
	public boolean hasIndents() {
		return !indents.isEmpty();
	}

	public Iterator<GrammarTree> indents() {
		return indents.iterator();
	}
	
	@Override
	public String toString() {
		return location() + ": " + reducedTo;
	}
}
