package org.flasck.flas.tc3;

import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.parsedForm.PolyType;
import org.flasck.flas.parsedForm.VarPattern;

public interface CurrentTCState {
	UnifiableType createUT();
	UnifiableType requireVarConstraints(InputPosition pos, String var);
	UnifiableType hasVar(String var);
	PolyType nextPoly(InputPosition pos);
	void argType(Type type);
	void bindVarToUT(String name, UnifiableType ty);
	void resolveAll(boolean hard);
	void bindVarPatternToUT(VarPattern vp, UnifiableType ty);
	void bindVarPatternTypes();
	Iterable<UnifiableType> unifiableTypes();
	void enhanceAllMutualUTs();
}
