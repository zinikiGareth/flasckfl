package org.flasck.flas.lifting;

import java.util.Map;
import java.util.TreeMap;

import org.flasck.flas.commonBase.names.FunctionName;
import org.flasck.flas.parsedForm.FunctionDefinition;
import org.zinutils.collections.SetMap;

public class LiftingDependencyMapper implements VarDependencyMapper {
	private Map<FunctionName, FunctionDefinition> functions = new TreeMap<>();
	private SetMap<FunctionName, FunctionName> dependencies = new SetMap<>();

	public void recordFunction(FunctionDefinition fn) {
		functions.put(fn.name(), fn);
		if (dependencies.contains(fn.name())) {
			for (FunctionName user : dependencies.get(fn.name())) {
				FunctionDefinition provider = functions.get(user);
				MappingStore ms = (MappingStore)provider.nestedVars();
				if (ms == null) {
					ms = new MappingStore();
					provider.nestedVars(ms);
				}
				ms.recordDependency(fn);
			}
			dependencies.removeAll(fn.name());
		}
	}
	
	@Override
	public void recordVarDependency(FunctionName user, FunctionName provider, MappingCollector collector) {
		if (functions.containsKey(provider)) {
			// we've already seen the provider, so we should be able to recover it ...
			FunctionDefinition pf = functions.get(provider);
			collector.recordDependency(pf);
		} else {
			// this is logically "collector.recordDependency(lookup(provider)); // except we have to wait for provider to turn up
			dependencies.add(provider, user);
		}
	}
}
