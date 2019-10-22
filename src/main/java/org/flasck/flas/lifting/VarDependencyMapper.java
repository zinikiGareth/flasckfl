package org.flasck.flas.lifting;

import org.flasck.flas.commonBase.names.FunctionName;

public interface VarDependencyMapper {

	void recordVarDependency(FunctionName name, FunctionName scope, MappingCollector collector);

}
