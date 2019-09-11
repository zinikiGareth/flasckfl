package org.flasck.flas.patterns;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.flasck.flas.commonBase.names.VarName;
import org.flasck.flas.repository.RepositoryReader;
import org.flasck.flas.tc3.Type;

public class HSIPatternOptions implements HSIOptions {
	private Map<String, HSITree> ctors = new TreeMap<>();
	private List<String> vars = new ArrayList<>();

	@Override
	public void addCM(String ctor, HSITree nested) {
		ctors.put(ctor, nested);
	}
	
	public void addVar(VarName varName) {
		String key = varName.uniqueName();
		vars.add(key);
	}

	@Override
	public HSITree getCM(String constructor) {
		return ctors.get(constructor);
	}

	@Override
	public List<String> vars() {
		return vars;
	}

	@Override
	public Type minimalType(RepositoryReader repository) {
		if (ctors.isEmpty() && !vars.isEmpty())
			return repository.get("Any");
		else
			return repository.get("Nil");
	}
}
