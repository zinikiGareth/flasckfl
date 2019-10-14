package org.flasck.flas.tc3;

import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.parsedForm.PolyType;

import test.tc3.TypeMatcher;

public interface CurrentTCState {
	UnifiableType requireVarConstraints(InputPosition pos, String var);
	UnifiableType hasVar(String var);
	PolyType nextPoly(InputPosition pos);
	void argType(TypeMatcher named);
}
