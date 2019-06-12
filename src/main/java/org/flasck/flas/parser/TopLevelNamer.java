package org.flasck.flas.parser;

import org.flasck.flas.commonBase.names.CardName;
import org.flasck.flas.commonBase.names.SolidName;

public interface TopLevelNamer extends FunctionScopeNamer {
	CardName cardName(String text);
	SolidName solidName(String text);
}
