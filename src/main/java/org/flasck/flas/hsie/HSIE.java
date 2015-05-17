package org.flasck.flas.hsie;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map.Entry;

import org.flasck.flas.parsedForm.ContainsScope;
import org.flasck.flas.parsedForm.FunctionCaseDefn;
import org.flasck.flas.parsedForm.FunctionDefinition;
import org.flasck.flas.parsedForm.Scope;
import org.flasck.flas.vcode.hsieForm.HSIEBlock;
import org.flasck.flas.vcode.hsieForm.HSIEForm;
import org.flasck.flas.vcode.hsieForm.HSIEForm.Var;

public class HSIE {
	public static HSIEForm handle(FunctionDefinition defn) {
		MetaState ms = new MetaState();
		HSIEForm ret = new HSIEForm();
		// build a state with the current set of variables and the list of patterns => expressions that they deal with
		ms.add(buildFundamentalState(ms, ret, defn.nargs, defn.cases));
		while (!ms.allDone()) {
			State f = ms.first();
			f.dump();
			recurse(ms, f);
		}
		ret.dump();
		return ret;
	}

	private static State buildFundamentalState(MetaState ms, HSIEBlock block, int nargs, List<FunctionCaseDefn> cases) {
		State s = new State(block);
		for (int i=0;i<nargs;i++) {
			Var v = ms.allocateVar();
			for (FunctionCaseDefn c : cases) {
				Object patt = c.args.get(i);
				s.associate(v, patt, new SubstExpr(c.expr));
			}
		}
		return s;
	}

	private static Collection<State> recurse(MetaState ms, State f) {
		List<State> ret = new ArrayList<State>();
		// TODO Auto-generated method stub
		return ret;
	}

	public static void applyTo(Scope s) {
		for (Entry<String, Object> x : s) {
			if (x instanceof FunctionDefinition) {
				handle((FunctionDefinition) x);
			}
			if (x instanceof ContainsScope)
				applyTo(((ContainsScope)x).innerScope());
		}
	}

}
