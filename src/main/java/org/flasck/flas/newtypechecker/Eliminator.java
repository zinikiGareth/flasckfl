package org.flasck.flas.newtypechecker;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.flasck.flas.vcode.hsieForm.Var;
import org.zinutils.collections.SetMap;
import org.zinutils.exceptions.NotImplementedException;
import org.zinutils.exceptions.UtilException;

public class Eliminator {
	private final SetMap<Var, TypeInfo> constraints;
	private final Map<Var, Var> renames;

	public Eliminator(SetMap<Var, TypeInfo> constraints, Map<Var, Var> renames) {
		this.constraints = constraints;
		this.renames = renames;
	}

	public void subst(Var k, Var v) {
		TypeChecker2.logger.debug("Replacing " + k + " with " + v + " everywhere");
		if (renames.containsKey(k))
			throw new UtilException("Cannot rename more than once");
		if (renames.containsKey(v))
			throw new UtilException("Cannot rename to a renamed var");
		renames.put(k, v);
		for (Entry<Var, Var> q : renames.entrySet())
			if (q.getValue().equals(k)) {
				renames.put(q.getKey(), v);
			}
		constraints.addAll(v, constraints.get(k));
		constraints.removeAll(k);
		for (Var m : constraints) {
			Set<TypeInfo> cm = constraints.get(m);
			Set<TypeInfo> tmp = new HashSet<>(cm);
			cm.clear();
			for (TypeInfo ti : tmp) {
				constraints.add(m, substType(k, v, ti));
			}
		}
	}

	protected TypeInfo substType(Var k, Var v, TypeInfo ti) {
		if (ti instanceof TypeVar) {
			TypeVar tv = (TypeVar) ti;
			if (tv.var.equals(k))
				return new TypeVar(tv.location(), v);
			else
				return tv;
		} else if (ti instanceof NamedType) {
			NamedType nt = (NamedType) ti;
			List<TypeInfo> polys = new ArrayList<TypeInfo>();
			for (TypeInfo pi : nt.polyArgs)
				polys.add(substType(k, v, pi));
			return new NamedType(nt.location(), nt.name, polys);
		} else if (ti instanceof TypeFunc) {
			TypeFunc tf = (TypeFunc) ti;
			List<TypeInfo> args = new ArrayList<>();
			for (TypeInfo ai : tf.args)
				args.add(ai);
			return new TypeFunc(tf.location(), args);
		} else if (ti instanceof TupleInfo) {
			TupleInfo tf = (TupleInfo) ti;
			List<TypeInfo> args = new ArrayList<>();
			for (TypeInfo ai : tf.args)
				args.add(ai);
			return new TupleInfo(tf.location(), args);
		} else if (ti instanceof PolyInfo) {
			return ti;
		} else
			throw new NotImplementedException(ti.getClass().getName());
	}

}
