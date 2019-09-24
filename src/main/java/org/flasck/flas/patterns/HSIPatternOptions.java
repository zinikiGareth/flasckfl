package org.flasck.flas.patterns;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import org.flasck.flas.commonBase.names.VarName;
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

		public TV(Type type, VarName var) {
			this.type = type;
			this.var = var;
		}
	}
	private List<String> vars = new ArrayList<>();
	private Map<String, TV> types = new TreeMap<>(); 
	private Map<String, HSITree> ctors = new TreeMap<>();

	@Override
	public void addCM(String ctor, HSITree nested) {
		ctors.put(ctor, nested);
	}

	@Override
	public void addTyped(TypeReference tr, VarName varName) {
		types.put(tr.name(), new TV((Type)tr.defn(), varName));
	}
	
	@Override
	public void addVar(VarName varName) {
		String key = varName.uniqueName();
		vars.add(key);
	}

	@Override
	public HSITree getCM(String constructor) {
		return ctors.get(constructor);
	}

	@Override
	public Set<String> ctors() {
		return ctors.keySet();
	}

	@Override
	public List<String> vars() {
		return vars;
	}

	@Override
	public List<Type> types() {
		List<Type> ret = new ArrayList<>();
		for (TV t : types.values())
			ret.add(t.type);
		return ret;
	}

	@Override
	public Type minimalType(CurrentTCState state, RepositoryReader repository) {
		if (ctors.size() == 1 && types.isEmpty() && vars.isEmpty())
			return repository.get(ctors.keySet().iterator().next());
		else if (ctors.isEmpty() && types.size() == 1 && vars.isEmpty())
			return types.values().iterator().next().type;
		else if (ctors.isEmpty() && types.isEmpty() && !vars.isEmpty()) {
			// TODO: need to consolidate all the vars in this slot
			UnifiableType ut = state.hasVar(vars.get(0));
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
	public boolean hasSwitches() {
		return !this.ctors.isEmpty();
	}
}
