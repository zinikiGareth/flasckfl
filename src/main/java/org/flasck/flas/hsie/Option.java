package org.flasck.flas.hsie;

import java.util.HashSet;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;

import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.parsedForm.ConstPattern;
import org.flasck.flas.parsedForm.ConstructorMatch.Field;
import org.flasck.flas.parsedForm.ExternalRef;
import org.flasck.flas.vcode.hsieForm.Var;
import org.zinutils.collections.ListMap;

public class Option {
	public final Var var;
	public final ListMap<ExternalRef, NestedBinds> ctorCases = new ListMap<ExternalRef, NestedBinds>(new ExternalRef.Comparator());
	public final Set<SubstExpr> undecidedCases = new HashSet<SubstExpr>();

	public Option(Var var) {
		this.var = var;
	}

	public void ifCtor(InputPosition location, ExternalRef ctor, List<Field> args, SubstExpr substExpr) {
		ctorCases.add(ctor, new NestedBinds(location, args, substExpr));
	}

	public void ifConst(ExternalRef ctor, ConstPattern cp, SubstExpr substExpr) {
		ctorCases.add(ctor, new NestedBinds(cp.location, cp, substExpr));
	}

	public void anything(SubstExpr value, String varToSubst) {
		undecidedCases.add(value);
	}

	public boolean dull() {
		return ctorCases.isEmpty();
	}
	
	public double valuation() {
		double tot = 0;
		for (ExternalRef x : ctorCases.keySet())
			tot += ctorCases.size(x);
		return tot/ctorCases.keySet().size() + undecidedCases.size();
	}
	
	public void dump() {
		System.out.println("Option " + var + "[" + valuation() + "] ->");
		for (Entry<ExternalRef, List<NestedBinds>> e : ctorCases.entrySet()) {
			System.out.println("  " + e.getKey() + " ->");
			for (NestedBinds nb : e.getValue())
				nb.dump();
		}
		if (!undecidedCases.isEmpty()) {
			System.out.println("  * ->");
			for (SubstExpr e : undecidedCases)
				System.out.println("    " + e);
		}
	}
}
