package org.flasck.flas.parser;

import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.commonBase.names.CardName;
import org.flasck.flas.commonBase.names.FunctionName;
import org.flasck.flas.commonBase.names.HandlerName;
import org.flasck.flas.commonBase.names.NameOfThing;
import org.flasck.flas.commonBase.names.PackageName;
import org.flasck.flas.commonBase.names.SolidName;

public class PackageNamer implements TopLevelNamer {
	private final NameOfThing pkg;

	public PackageNamer(String pkg) {
		this.pkg = new PackageName(pkg);
	}

	public PackageNamer(NameOfThing pkgName) {
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

	@Override
	public CardName cardName(String text) {
		return new CardName((PackageName) pkg, text);
	}

	@Override
	public SolidName solidName(String text) {
		return new SolidName(pkg, text);
	}
}
