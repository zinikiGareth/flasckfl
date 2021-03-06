package org.flasck.flas.parser;

import org.flasck.flas.commonBase.names.AssemblyName;
import org.flasck.flas.commonBase.names.CardName;
import org.flasck.flas.commonBase.names.ObjectName;
import org.flasck.flas.commonBase.names.SolidName;

public interface TopLevelNamer extends FunctionScopeNamer {
	CardName cardName(String text);
	SolidName solidName(String text);
	ObjectName objectName(String text);
	AssemblyName assemblyName(String name);
}
