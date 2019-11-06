package org.flasck.flas.patterns;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import org.flasck.flas.commonBase.names.VarName;
import org.flasck.flas.parsedForm.FunctionIntro;
import org.flasck.flas.parsedForm.StructDefn;
import org.flasck.flas.parsedForm.TypeReference;
import org.flasck.flas.parsedForm.TypedPattern;
import org.flasck.flas.parsedForm.VarPattern;
import org.flasck.flas.repository.FunctionHSICases;
import org.flasck.flas.repository.HSICases;
import org.flasck.flas.repository.LoadBuiltins;
import org.flasck.flas.repository.RepositoryReader;
import org.flasck.flas.tc3.CurrentTCState;
import org.flasck.flas.tc3.NamedType;
import org.flasck.flas.tc3.Primitive;
import org.flasck.flas.tc3.Type;
import org.flasck.flas.tc3.UnifiableType;
import org.zinutils.exceptions.NotImplementedException;

public class HSIPatternOptions implements HSIOptions {
	class TV {
		NamedType type;
		TypedPattern tp;
		VarPattern vp;
		VarName var;
		List<FunctionIntro> intros = new ArrayList<>();

		public TV(TypedPattern tp) {
			this.type = (NamedType) tp.type.defn();
			this.tp = tp;
			this.var = tp.var;
		}

		public TV(NamedType type, VarPattern vp) {
			this.type = type;
			this.vp = vp;
			this.var = vp.name();
		}

		public TV(NamedType type, VarName var) {
			this.type = type;
			this.var = var;
		}

		public boolean containsAny(HSICases curr) {
			for (FunctionIntro fi : intros)
				if (curr.contains(fi))
					return true;
			return false;
		}
	}
	private List<FunctionIntro> all = new ArrayList<>();
	private List<TV> vars = new ArrayList<>();
	private Map<NamedType, TV> types = new TreeMap<>(NamedType.nameComparator); 
	private Map<StructDefn, HSICtorTree> ctors = new TreeMap<>(StructDefn.nameComparator);
	private Set<Integer> numericConstants = new TreeSet<>();
	private Set<String> stringConstants = new TreeSet<>();

	@Override
	public void includes(FunctionIntro fi) {
		all.add(fi);
	}
	
	@Override
	public HSICtorTree requireCM(StructDefn ctor) {
		if (!ctors.containsKey(ctor))
			ctors.put(ctor, new HSICtorTree());
		return ctors.get(ctor);
	}

	@Override
	public void addTyped(TypedPattern tp, FunctionIntro fi) {
		NamedType td = (NamedType) tp.type.defn();
		if (td == null)
			throw new RuntimeException("No definition in " + tp.type);
		types.put(td, new TV(tp));
		types.get(td).intros.add(fi);
	}
	
	@Override
	public void addVar(VarPattern vp, FunctionIntro fi) {
		TV tv = new TV(null, vp);
		tv.intros.add(fi);
		vars.add(tv);
	}
	
	@Override
	public void addVarWithType(TypeReference tr, VarName varName, FunctionIntro fi) {
		TV tv = new TV((NamedType) tr.defn(), varName);
		tv.intros.add(fi);
		vars.add(tv);
	}
	
	@Override
	public void addConstant(Primitive type, String value, FunctionIntro fi) {
		types.put(type, new TV(type, (VarName)null));
		types.get(type).intros.add(fi);
		if (type.name().uniqueName().equals("Number"))
			numericConstants.add(Integer.parseInt(value));
		else if (type.name().uniqueName().equals("String"))
			stringConstants.add(value);
		else
			throw new NotImplementedException("Cannot handle const of type " + type);
	}

	@Override
	public HSITree getCM(StructDefn constructor) {
		return ctors.get(constructor);
	}

	@Override
	public List<FunctionIntro> getIntrosForType(NamedType ty) {
		return types.get(ty).intros;
	}

	@Override
	public List<FunctionIntro> getDefaultIntros(HSICases cases) {
		ArrayList<FunctionIntro> ret = new ArrayList<>(all);
		for (HSICtorTree ct : ctors.values())
			ret.removeAll(ct.intros());
		for (TV tv : types.values())
			ret.removeAll(tv.intros);
		return ret;
	}

	@Override
	public Set<StructDefn> ctors() {
		return ctors.keySet();
	}

	@Override
	public List<IntroTypeVar> typedVars(NamedType ty) {
		TV tv = types.get(ty);
		List<IntroTypeVar> ret = new ArrayList<>();
		if (tv.intros.size() != 1)
			throw new RuntimeException("I wasn't expecting that");
		IntroTypeVar itv;
		if (tv.tp != null)
			itv = new IntroTypeVar(tv.intros.get(0), tv.tp);
		else
			itv = new IntroTypeVar(tv.intros.get(0), tv.type);
		ret.add(itv);
		return ret;
	}

	@Override
	public List<IntroVarName> vars() {
		List<IntroVarName> ret = new ArrayList<>();
		for (TV v : vars) {
			if (v.intros.size() != 1)
				throw new RuntimeException("I wasn't expecting that");
			IntroVarName iv;
			if (v.vp != null)
				iv = new IntroVarName(v.intros.get(0), v.vp);
			else
				iv = new IntroVarName(v.intros.get(0), v.var);
			ret.add(iv);
		}
		return ret;
	}
	
	@Override
	public List<IntroVarName> vars(HSICases intros) {
		List<IntroVarName> ret = new ArrayList<>();
		addVars(ret, intros, vars);
		addVars(ret, intros, types.values());
		return ret;
	}

	private void addVars(List<IntroVarName> ret, HSICases intros, Collection<TV> list) {
		for (TV v : list) {
			for (FunctionIntro i : ((FunctionHSICases)intros).intros) {
				if (v.intros.contains(i))
					if (v.var != null)
						ret.add(new IntroVarName(i, v.var));
			}
		}
	}

	@Override
	public Set<Integer> numericConstants(HSICases intersect) {
		return numericConstants;
	}

	@Override
	public Set<String> stringConstants(HSICases intersect) {
		return stringConstants;
	}

	@Override
	public Set<NamedType> types() {
		return types.keySet();
	}

	@Override
	public Type minimalType(CurrentTCState state, RepositoryReader repository) {
		List<TV> vs = new ArrayList<>();
		Map<NamedType, TV> ts = new TreeMap<>(NamedType.nameComparator);
		ts.putAll(types);
		for (TV v : vars) {
			if (v.type == null)
				vs.add(v);
			else
				ts.put((NamedType)v.type, v);
		}
		if (ctors.size() == 1 && ts.isEmpty())
			return ctors.keySet().iterator().next();
		else if (ctors.isEmpty() && ts.size() == 1)
			return ts.values().iterator().next().type;
		else if (ts.containsKey(LoadBuiltins.any))
			return LoadBuiltins.any;
		else if (ctors.isEmpty() && ts.isEmpty() && !vs.isEmpty()) {
			// TODO: need to consolidate all the vars in this slot
			UnifiableType ut = state.hasVar(vs.get(0).var.uniqueName());
			if (ut == null)
				return LoadBuiltins.any;
			else
				return ut.resolve();
		} else {
			Set<Type> ms = new HashSet<>(ctors.keySet());
			Type ut = repository.findUnionWith(ms);
			if (ut == null) {
				return repository.get("Any");
//				throw new NotImplementedException("Could not find union for " + ms);
			}
			return ut;
		}
	}

	@Override
	public boolean hasSwitches(HSICases intros) {
		for (HSITree c : this.ctors.values())
			if (c.containsAny(intros))
				return true;
		for (TV c : this.types.values())
			if (c.containsAny(intros))
				return true;
		return false;
	}
	
	@Override
	public int score() {
		int score = types.size() + ctors.size()*3;
		// Any is not really a restriction and we shouldn't really switch on it
		// But it makes the typechecker "happy".
		if (types.containsKey(LoadBuiltins.any))
			score--;
		return score;
	}
}
