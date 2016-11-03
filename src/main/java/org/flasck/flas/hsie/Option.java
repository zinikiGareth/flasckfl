package org.flasck.flas.hsie;

import java.util.HashSet;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;

import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.commonBase.ConstPattern;
import org.flasck.flas.rewrittenForm.RWConstructorMatch.Field;
import org.flasck.flas.vcode.hsieForm.Var;
import org.slf4j.Logger;
import org.zinutils.collections.ListMap;
import org.zinutils.exceptions.UtilException;
import org.zinutils.utils.StringComparator;

public class Option {
	public final Var var;
	public final ListMap<String, NestedBinds> ctorCases = new ListMap<String, NestedBinds>(new StringComparator());
	public final Set<Integer> undecidedCases = new HashSet<Integer>();
	public InputPosition location;

	public Option(InputPosition loc, Var var) {
		if (loc == null)
			throw new UtilException("location cannot be null");
		location = loc;
		this.var = var;
	}

	public void ifCtor(InputPosition location, String ctor, List<Field> args, int substExpr) {
		ctorCases.add(ctor, new NestedBinds(location, args, substExpr));
	}

	public void ifConst(String ctor, ConstPattern cp, int substExpr) {
		ctorCases.add(ctor, new NestedBinds(cp.location, cp, substExpr));
	}

	public void anything(int value, String varToSubst) {
		undecidedCases.add(value);
	}

	public boolean dull() {
		return ctorCases.isEmpty();
	}
	
	public double valuation() {
		double tot = 0;
		for (String x : ctorCases.keySet())
			tot += ctorCases.size(x);
		return tot/ctorCases.keySet().size() + undecidedCases.size();
	}
	
	public void dump() {
		System.out.println("Option " + var + "[" + valuation() + "] ->");
		for (Entry<String, List<NestedBinds>> e : ctorCases.entrySet()) {
			System.out.println("  " + e.getKey() + " ->");
			for (NestedBinds nb : e.getValue())
				System.out.println(nb.dump());
		}
		if (!undecidedCases.isEmpty()) {
			System.out.println("  * ->");
			for (Object e : undecidedCases)
				System.out.println("    " + e);
		}
	}
	
	public void dump(Logger logger) {
		logger.info("Option " + var + "[" + valuation() + "] ->");
		for (Entry<String, List<NestedBinds>> e : ctorCases.entrySet()) {
			logger.info("  " + e.getKey() + " ->");
			for (NestedBinds nb : e.getValue())
				logger.info(nb.dump());
		}
		if (!undecidedCases.isEmpty()) {
			logger.info("  * ->");
			for (Object e : undecidedCases)
				logger.info("    " + e);
		}
	}
}
