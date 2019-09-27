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
	}
	private List<VarName> vars = new ArrayList<>();
	private Map<String, TV> types = new TreeMap<>(); 
	private Map<String, HSITree> ctors = new TreeMap<>();

	@Override
	public HSITree requireCM(String ctor, int nargs) {
		if (!ctors.containsKey(ctor))
			ctors.put(ctor, new HSIPatternTree(nargs));
		return ctors.get(ctor);
	}

	@Override
	public void addTyped(TypeReference tr, VarName varName, FunctionIntro fi) {
		types.put(tr.name(), new TV((Type)tr.defn(), varName));
		types.get(tr.name()).intros.add(fi);
	}
	
	@Override
	public void addVar(VarName varName) {
		vars.add(varName);
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
	public Set<String> ctors() {
		return ctors.keySet();
	}

	@Override
	public List<VarName> vars() {
		return vars;
	}

	@Override
	public Set<String> types() {
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
			UnifiableType ut = state.hasVar(vars.get(0).uniqueName());
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
		return !this.ctors.isEmpty() || !this.types.isEmpty();
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
