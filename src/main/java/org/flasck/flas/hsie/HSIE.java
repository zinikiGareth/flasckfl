package org.flasck.flas.hsie;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map.Entry;

import org.flasck.flas.parsedForm.ConstPattern;
import org.flasck.flas.parsedForm.ConstructorMatch;
import org.flasck.flas.parsedForm.ConstructorMatch.Field;
import org.flasck.flas.parsedForm.ContainsScope;
import org.flasck.flas.parsedForm.FunctionCaseDefn;
import org.flasck.flas.parsedForm.FunctionDefinition;
import org.flasck.flas.parsedForm.Scope;
import org.flasck.flas.parsedForm.VarPattern;
import org.flasck.flas.vcode.hsieForm.HSIEBlock;
import org.flasck.flas.vcode.hsieForm.HSIEForm;
import org.flasck.flas.vcode.hsieForm.HSIEForm.Var;
import org.zinutils.exceptions.UtilException;

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

	private static void recurse(MetaState ms, State s) {
		Table t = buildDecisionTable(s);
		t.dump();
	}

	private static Table buildDecisionTable(State s) {
		Table t = new Table();
		for (Entry<Var, PattExpr> e : s) {
			Option o = t.createOption(e.getKey());
			for (Entry<Object, SubstExpr> pe : e.getValue()) {
				Object patt = pe.getKey();
				if (patt instanceof VarPattern) {
					o.anything(pe.getValue(), ((VarPattern)patt).var);
				} else if (patt instanceof ConstructorMatch) {
					ConstructorMatch cm = (ConstructorMatch) patt;
					o.ifCtor(cm.ctor, cm.args, pe.getValue());
				} else if (patt instanceof ConstPattern) {
					ConstPattern cp = (ConstPattern) patt;
					if (cp.type == ConstPattern.INTEGER) {
						o.ifCtor("Number", new ArrayList<Field>(), pe.getValue()); // somewhere we need to attach an "IF" but that's for later
					} else
						throw new UtilException("Cannot handle constant pattern for " + cp.type);
				} else
					System.out.println("Cannot handle pattern " + patt.getClass());
			}
		}
		return t;
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
