package org.flasck.flas.patterns;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import org.flasck.flas.commonBase.names.VarName;
import org.flasck.flas.parsedForm.FunctionIntro;
import org.flasck.flas.parsedForm.TypeReference;
import org.flasck.flas.repository.RepositoryReader;
import org.flasck.flas.tc3.CurrentTCState;
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
	private List<TV> vars = new ArrayList<>();
	private Map<String, TV> types = new TreeMap<>(); 
	private Map<String, HSICtorTree> ctors = new TreeMap<>();

	@Override
	public HSICtorTree requireCM(String ctor) {
		if (!ctors.containsKey(ctor))
			ctors.put(ctor, new HSICtorTree());
		return ctors.get(ctor);
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
	public HSITree getCM(String constructor) {
		return ctors.get(constructor);
	}

	@Override
	public List<FunctionIntro> getIntrosForType(String ty) {
		return types.get(ty).intros;
	}

	@Override
	public List<FunctionIntro> getDefaultIntros() {
		// I think the stored structure is back to front, and we should first be figuring out which intro, and then saying "ah, in that case the vars are" ...
		// but I don't have unit tests to back that up
		// When I do, I think this becomes easier, but there may be other logic
		ArrayList<FunctionIntro> ret = new ArrayList<>();
		for (TV tv : vars)
			ret.addAll(tv.intros);
		return ret;
	}

	@Override
	public Set<String> ctors() {
		return ctors.keySet();
	}

	@Override
	public List<VarName> vars(List<FunctionIntro> intros) {
		if (intros.size() != 1)
			throw new RuntimeException("This is an error: either we have started binding before switching, or there are multiple overlapping cases");
		FunctionIntro i = intros.get(0);
		List<VarName> ret = new ArrayList<VarName>();
		for (TV v : vars)
			if (v.intros.contains(i))
				ret.add(v.var);
		return ret;
	}

	@Override
	public Set<String> types(List<FunctionIntro> intros) {
		return types.keySet();
	}

	@Override
	public Type minimalType(CurrentTCState state, RepositoryReader repository) {
		if (ctors.size() == 1 && types.isEmpty())
			return repository.get(ctors.keySet().iterator().next());
		else if (ctors.isEmpty() && types.size() == 1)
			return types.values().iterator().next().type;
		else if (types.containsKey("Any"))
			return types.get("Any").type;
		else if (ctors.isEmpty() && types.isEmpty() && !vars.isEmpty()) {
			// TODO: need to consolidate all the vars in this slot
			UnifiableType ut = state.hasVar(vars.get(0).var.uniqueName());
			if (ut == null)
				return repository.get("Any");
			else
				return ut.resolve();
		} else {
			Set<String> ms = ctors.keySet();
			Type ut = repository.findUnionWith(ms);
			if (ut == null)
				throw new NotImplementedException("Could not find union for " + ms);
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
