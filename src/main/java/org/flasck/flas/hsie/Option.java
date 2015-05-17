package org.flasck.flas.hsie;

import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;

import org.flasck.flas.parsedForm.ConstructorMatch.Field;
import org.flasck.flas.vcode.hsieForm.HSIEForm.Var;
import org.zinutils.collections.ListMap;

public class Option {
	private final Var var;
	private final ListMap<String, NestedBinds> ctorCases = new ListMap<String, NestedBinds>();
	private final List<SubstExpr> undecidedCases = new ArrayList<SubstExpr>();

	public Option(Var var) {
		this.var = var;
	}

	public void ifCtor(String ctor, List<Field> args, SubstExpr substExpr) {
		ctorCases.add(ctor, new NestedBinds(args, substExpr));
	}

	public void anything(SubstExpr value, String varToSubst) {
		undecidedCases.add(value.cloneWith(varToSubst, var));
	}

	public void dump() {
		System.out.println("Option " + var + " ->");
		for (Entry<String, List<NestedBinds>> e : ctorCases.entrySet()) {
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
