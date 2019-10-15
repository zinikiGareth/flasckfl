package org.flasck.flas.tc3;

import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.parsedForm.PolyType;

public interface CurrentTCState {
	UnifiableType nextArg();
	UnifiableType requireVarConstraints(InputPosition pos, String var);
	UnifiableType hasVar(String var);
	PolyType nextPoly(InputPosition pos);
	void argType(Type type);
}
