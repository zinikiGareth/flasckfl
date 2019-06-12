package org.flasck.flas.parser;

import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.commonBase.names.FunctionName;
import org.flasck.flas.commonBase.names.HandlerName;
import org.flasck.flas.commonBase.names.NameOfThing;
import org.flasck.flas.commonBase.names.PackageName;

public class InnerPackageNamer implements FunctionScopeNamer {
	protected final NameOfThing pkg;

	public InnerPackageNamer(String pkg) {
		this.pkg = new PackageName(pkg);
	}

	public InnerPackageNamer(NameOfThing pkgName) {
		this.pkg = pkgName;
	}

	@Override
	public FunctionName functionName(InputPosition location, String base) {
		return FunctionName.function(location, pkg, base);
	}

	@Override
	public HandlerName handlerName(String baseName) {
		return new HandlerName(pkg, baseName);
	}
}
