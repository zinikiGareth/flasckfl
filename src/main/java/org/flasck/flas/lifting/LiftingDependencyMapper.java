package org.flasck.flas.lifting;

import java.util.Map;
import java.util.TreeMap;

import org.flasck.flas.commonBase.names.FunctionName;
import org.flasck.flas.parsedForm.StandaloneDefn;
import org.flasck.flas.parsedForm.StandaloneMethod;
import org.zinutils.collections.SetMap;

public class LiftingDependencyMapper implements VarDependencyMapper {
	private Map<FunctionName, StandaloneDefn> functions = new TreeMap<>();
	private SetMap<FunctionName, FunctionName> dependencies = new SetMap<>();

	public void recordFunction(StandaloneDefn fn) {
		functions.put(fn.name(), fn);
		if (dependencies.contains(fn.name())) {
			for (FunctionName user : dependencies.get(fn.name())) {
				StandaloneDefn provider = functions.get(user);
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
	
	public void recordMethod(StandaloneMethod meth) {
		
	}
	
	@Override
	public void recordVarDependency(FunctionName user, FunctionName provider, MappingCollector collector) {
		if (functions.containsKey(provider)) {
			// we've already seen the provider, so we should be able to recover it ...
			StandaloneDefn pf = functions.get(provider);
			collector.recordDependency(pf);
		} else {
			// this is logically "collector.recordDependency(lookup(provider)); // except we have to wait for provider to turn up
			dependencies.add(provider, user);
		}
	}
}
