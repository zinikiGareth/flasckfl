package org.flasck.flas.patterns;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import org.flasck.flas.commonBase.names.NamedThing;
import org.flasck.flas.commonBase.names.VarName;
import org.flasck.flas.parsedForm.FunctionIntro;
import org.flasck.flas.parsedForm.StructDefn;
import org.flasck.flas.parsedForm.TypeReference;
import org.flasck.flas.repository.RepositoryReader;
import org.flasck.flas.tc3.CurrentTCState;
import org.flasck.flas.tc3.Primitive;
import org.flasck.flas.tc3.Type;
import org.flasck.flas.tc3.UnifiableType;
import org.zinutils.exceptions.NotImplementedException;

public class HSIPatternOptions implements HSIOptions {
	class TV {
		Type type;
		VarName var;
		List<FunctionIntro> intros = new ArrayList<>();

		public TV(Type type, VarName var) {
			this.type = type;
			this.var = var;
		}

		public boolean containsAny(List<FunctionIntro> curr) {
			for (FunctionIntro fi : intros)
				if (curr.contains(fi))
					return true;
			return false;
		}
	}
	private List<FunctionIntro> all = new ArrayList<>();
	private List<TV> vars = new ArrayList<>();
	private Map<String, TV> types = new TreeMap<>(); 
	private Map<String, HSICtorTree> ctors = new TreeMap<>();
	private Set<Integer> numericConstants = new TreeSet<>();
	private Set<String> stringConstants = new TreeSet<>();

	@Override
	public void includes(FunctionIntro fi) {
		all.add(fi);
	}
	
	@Override
	public HSICtorTree requireCM(StructDefn ctor) {
		String name = ctor.name.uniqueName();
		if (!ctors.containsKey(name))
			ctors.put(name, new HSICtorTree());
		return ctors.get(name);
	}

	@Override
	public void addTyped(TypeReference tr, VarName varName, FunctionIntro fi) {
		types.put(tr.name(), new TV((Type)tr.defn(), varName));
		types.get(tr.name()).intros.add(fi);
	}
	
	@Override
	public void addVar(VarName varName, FunctionIntro fi) {
		TV tv = new TV(null, varName);
		tv.intros.add(fi);
		vars.add(tv);
	}
	
	@Override
	public void addVarWithType(TypeReference tr, VarName varName, FunctionIntro fi) {
		TV tv = new TV((Type) tr.defn(), varName);
		tv.intros.add(fi);
		vars.add(tv);
	}
	
	@Override
	public void addConstant(Primitive type, String value, FunctionIntro fi) {
		String tn = type.name().uniqueName();
		types.put(tn, new TV(type, null));
		types.get(tn).intros.add(fi);
		if (type.name().uniqueName().equals("Number"))
			numericConstants.add(Integer.parseInt(value));
		else if (type.name().uniqueName().equals("String"))
			stringConstants.add(value);
		else
			throw new NotImplementedException("Cannot handle const of type " + type);
	}

	@Override
	public HSITree getCM(String constructor) {
		return ctors.get(constructor);
	}

	@Override
	public List<FunctionIntro> getIntrosForType(String ty) {
		return types.get(ty).intros;
	}

	@Override
	public List<FunctionIntro> getDefaultIntros(List<FunctionIntro> intros) {
		ArrayList<FunctionIntro> ret = new ArrayList<>(all);
		for (HSICtorTree ct : ctors.values())
			ret.removeAll(ct.intros());
		for (TV tv : types.values())
			ret.removeAll(tv.intros);
		return ret;
	}

	@Override
	public Set<String> ctors() {
		return ctors.keySet();
	}

	@Override
	public List<IntroVarName> vars(List<FunctionIntro> intros) {
		List<IntroVarName> ret = new ArrayList<>();
		addVars(ret, intros, vars);
		addVars(ret, intros, types.values());
		return ret;
	}

	private void addVars(List<IntroVarName> ret, List<FunctionIntro> intros, Collection<TV> list) {
		for (TV v : list) {
			for (FunctionIntro i : intros) {
				if (v.intros.contains(i))
					if (v.var != null)
						ret.add(new IntroVarName(i, v.var));
			}
		}
	}

	@Override
	public Set<Integer> numericConstants(ArrayList<FunctionIntro> intersect) {
		return numericConstants;
	}

	@Override
	public Set<String> stringConstants(ArrayList<FunctionIntro> intersect) {
		return stringConstants;
	}

	@Override
	public Set<String> types(List<FunctionIntro> intros) {
		return types.keySet();
	}

	@Override
	public Type minimalType(CurrentTCState state, RepositoryReader repository) {
		List<TV> vs = new ArrayList<>();
		Map<String, TV> ts = new TreeMap<>(types);
		for (TV v : vars) {
			if (v.type == null)
				vs.add(v);
			else
				ts.put(((NamedThing)v.type).getName().uniqueName(), v);
		}
		if (ctors.size() == 1 && ts.isEmpty())
			return repository.get(ctors.keySet().iterator().next());
		else if (ctors.isEmpty() && ts.size() == 1)
			return ts.values().iterator().next().type;
		else if (ts.containsKey("Any"))
			return ts.get("Any").type;
		else if (ctors.isEmpty() && ts.isEmpty() && !vs.isEmpty()) {
			// TODO: need to consolidate all the vars in this slot
			UnifiableType ut = state.hasVar(vs.get(0).var.uniqueName());
			if (ut == null)
				return repository.get("Any");
			else
				return ut.resolve();
		} else {
			Set<String> ms = ctors.keySet();
			Type ut = repository.findUnionWith(ms);
			if (ut == null) {
				return repository.get("Any");
//				throw new NotImplementedException("Could not find union for " + ms);
			}
			return ut;
		}
	}

	@Override
	public boolean hasSwitches(List<FunctionIntro> intros) {
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
		if (types.containsKey("Any"))
			score--;
		return score;
	}
}
