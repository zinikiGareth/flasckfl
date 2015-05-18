package org.flasck.flas.hsie;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.flasck.flas.vcode.hsieForm.HSIEBlock;
import org.flasck.flas.vcode.hsieForm.HSIEForm.Var;
import org.zinutils.exceptions.UtilException;

public class State implements Iterable<Entry<Var,PattExpr>> {
	private final Map<Var, PattExpr> mapping = new HashMap<Var, PattExpr>();
	public final HSIEBlock writeTo;
	private PattExpr result;
	
	public State(HSIEBlock b) {
		this.writeTo = b;
	}
	
	// Create a mapping from v -> patt -> substexpr
	public void associate(Var v, Object patt, SubstExpr expr) {
		if (v == null)
			throw new UtilException("Cannot use null var");
		if (!mapping.containsKey(v))
			mapping.put(v, new PattExpr());
		mapping.get(v).associate(patt, expr);
	}

	public State cloneEliminate(Var var, HSIEBlock into, Set<SubstExpr> possibles) {
		System.out.println("Eliminating " + var + " and allowing " + possibles);
		State ret = new State(into);
		for (Entry<Var, PattExpr> x : mapping.entrySet()) {
			System.out.println("Considering " + x.getKey());
			if (x.getKey().equals(var)) {
				if (mapping.size() == 1)
					ret.result = x.getValue();
				continue;
			}
			PattExpr pe = x.getValue().duplicate(possibles);
			System.out.println("Duplicated value is " + pe);
			if (pe != null)
				ret.mapping.put(x.getKey(), pe);
		}
		return ret;
	}
	
	public void eliminate(Var var) {
		if (!mapping.containsKey(var))
			throw new UtilException("Cannot eliminate " +var + " which is not present");
		if (mapping.size() == 1)
			result = mapping.get(var);
		mapping.remove(var);
	}

	public boolean hasNeeds() {
		return !mapping.isEmpty();
	}

	public SubstExpr singleExpr() {
		if (result == null)
			return null;
//			throw new UtilException("Didn't resolve to single result");
		return result.singleExpr();
	}

	public PattExpr get(Var var) {
		return mapping.get(var);
	}

	@Override
	public Iterator<Entry<Var, PattExpr>> iterator() {
		return mapping.entrySet().iterator();
	}

	public void dump() {
		for (Entry<Var, PattExpr> e : mapping.entrySet()) {
			System.out.println(e.getKey() + " -> ");
			e.getValue().dump();
		}
	}
}
