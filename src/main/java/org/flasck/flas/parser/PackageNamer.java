package org.flasck.flas.parser;

import org.flasck.flas.commonBase.names.CardName;
import org.flasck.flas.commonBase.names.NameOfThing;
import org.flasck.flas.commonBase.names.PackageName;
import org.flasck.flas.commonBase.names.SolidName;

public class PackageNamer extends InnerPackageNamer implements TopLevelNamer {
	public PackageNamer(String pkg) {
		this(new PackageName(pkg));
	}

	public PackageNamer(NameOfThing pkgName) {
		super(pkgName);
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
