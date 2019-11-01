package org.flasck.flas.parser;

import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.commonBase.names.FunctionName;
import org.flasck.flas.commonBase.names.HandlerName;
import org.flasck.flas.commonBase.names.NameOfThing;

public class InnerPackageNamer implements FunctionScopeNamer {
	protected final NameOfThing pkg;

	public InnerPackageNamer(NameOfThing pkgName) {
		this.pkg = pkgName;
	}

	@Override
	public FunctionName functionName(InputPosition location, String base) {
		return FunctionName.function(location, pkg, base);
	}

	@Override
	public FunctionName functionCase(InputPosition location, String base, int caseNum) {
		return FunctionName.caseName(functionName(location, base), caseNum);
	}

	@Override
	public HandlerName handlerName(String baseName) {
		return new HandlerName(pkg, baseName);
	}
}
