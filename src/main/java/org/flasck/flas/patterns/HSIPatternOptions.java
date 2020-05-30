package org.flasck.flas.patterns;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
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
import org.flasck.flas.tc3.NamedType;
import org.flasck.flas.tc3.PolyInstance;
import org.flasck.flas.tc3.Primitive;
import org.zinutils.exceptions.CantHappenException;
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

		public boolean isCompatibleIntro(FunctionIntro i) {
			// a semi-hack to deal with methods that store "null" instead of the real intro ...
			if (intros.size() == 1 && intros.get(0) == null)
				return true;
			return intros.contains(i);
		}
	}
	private List<FunctionIntro> all = new ArrayList<>();
	private List<TV> vars = new ArrayList<>();
	private Map<NamedType, TV> types = new TreeMap<>(NamedType.nameComparator); 
	private Map<StructDefn, HSICtorTree> ctors = new TreeMap<>(NamedType.nameComparator);
	private Set<Integer> numericConstants = new TreeSet<>();
	private Set<String> stringConstants = new TreeSet<>();
	private boolean container;

	@Override
	public boolean isContainer() {
		return container;
	}

	@Override
	public NamedType containerType() {
		return vars.get(0).type;
	}
	
	@Override
	public void includes(FunctionIntro fi) {
		if (!all.contains(fi))
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
		while (td instanceof PolyInstance)
			td = ((PolyInstance)td).struct();
		if (!types.containsKey(td))
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
		if (varName.baseName().equals("_this"))
			container = true;
	}
	
	@Override
	public void addConstant(Primitive type, String value, FunctionIntro fi) {
		if (!types.containsKey(type))
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
		while (ty instanceof PolyInstance)
			ty = ((PolyInstance)ty).struct();
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
		for (FunctionIntro intro : tv.intros) {
			IntroTypeVar itv;
			if (tv.tp != null)
				itv = new IntroTypeVar(intro, tv.tp);
			else
				itv = new IntroTypeVar(intro, tv.type);
			ret.add(itv);
		}
		return ret;
	}

	@Override
	public List<IntroVarName> vars() {
		List<IntroVarName> ret = new ArrayList<>();
		for (TV v : vars) {
			if (v.intros.size() != 1)
				throw new CantHappenException("There should only be one intro left");
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
				if (v.isCompatibleIntro(i))
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
	
	@Override
	public void dump(String indent) {
		boolean spoken = false;
		for (Entry<StructDefn, HSICtorTree> c : ctors.entrySet()) {
			System.out.println(indent + "ctor " + c.getKey().signature() + ":");
			c.getValue().dump(indent + "  ");
			spoken = true;
		}
		for (Entry<NamedType, TV> t : types.entrySet()) {
			System.out.println(indent + "type " + t.getKey().signature() + ":" + t.getValue().intros);
			spoken = true;
		}
		if (!spoken)
			System.out.println(indent + "all: " + all);
	}
}
