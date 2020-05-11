package org.flasck.flas.parser;

import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.commonBase.names.SolidName;
import org.flasck.flas.commonBase.names.VarName;

public interface VarNamer {
	VarName nameVar(InputPosition loc, String name);
	SolidName namePoly(InputPosition pos, String tok);
}
