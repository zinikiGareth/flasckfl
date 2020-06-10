package org.flasck.flas.parser;

import org.flasck.flas.commonBase.names.AssemblyName;
import org.flasck.flas.commonBase.names.CardName;
import org.flasck.flas.commonBase.names.NameOfThing;
import org.flasck.flas.commonBase.names.ObjectName;
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
	
	@Override
	public ObjectName objectName(String text) {
		return new ObjectName(pkg, text);
	}
	
	@Override
	public AssemblyName assemblyName(String name) {
		return new AssemblyName(pkg, name);
	}
}
